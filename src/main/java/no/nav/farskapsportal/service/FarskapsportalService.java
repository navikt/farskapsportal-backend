package no.nav.farskapsportal.service;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.BrukerinformasjonResponse;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.OppretteFarskaperklaeringRequest;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringResponse;
import no.nav.farskapsportal.config.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.DokumentStatusDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.SignaturDto;
import no.nav.farskapsportal.exception.FarskapserklaeringIkkeFunnetException;
import no.nav.farskapsportal.exception.HentingAvDokumentFeiletException;
import no.nav.farskapsportal.exception.ManglerRelasjonException;
import no.nav.farskapsportal.exception.MorHarIngenNyfoedteUtenFarException;
import no.nav.farskapsportal.exception.NyfoedtErForGammelException;
import no.nav.farskapsportal.exception.OppretteFarskapserklaeringException;
import no.nav.farskapsportal.exception.PersonHarFeilRolleException;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.util.MappingUtil;
import org.apache.commons.lang3.Validate;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Slf4j
public class FarskapsportalService {

  public static final String FEIL_NAVN = "Oppgitt navn til person stemmer ikke med navn slik det er registreret i Folkeregisteret";

  private final FarskapsportalEgenskaper farskapsportalEgenskaper;
  private final PdfGeneratorConsumer pdfGeneratorConsumer;
  private final DifiESignaturConsumer difiESignaturConsumer;
  private final PersistenceService persistenceService;
  private final PersonopplysningService personopplysningService;
  private final MappingUtil mappingUtil;

  public BrukerinformasjonResponse henteBrukerinformasjon(String fnrPaaloggetBruker) {

    // hente rolle
    var brukersForelderrolle = personopplysningService.bestemmeForelderrolle(fnrPaaloggetBruker);
    Set<FarskapserklaeringDto> avventerSignereringPaaloggetBruker = new HashSet<>();
    Set<FarskapserklaeringDto> avventerSigneringMotpart = new HashSet<>();
    Set<FarskapserklaeringDto> avventerRegistreringSkatt = new HashSet<>();
    Set<String> nyligFoedteBarnSomManglerFar = new HashSet<>();
    var kanOppretteFarskapserklaering = false;
    Optional<Feilkode> feilkodeTilgang;

    // Avbryte videre flyt dersom bruker ikke er myndig eller har en rolle som ikke støttes av løsningen
    feilkodeTilgang = vurdereTilgangBasertPaaAlderOgForeldrerolle(fnrPaaloggetBruker, brukersForelderrolle);
    if (feilkodeTilgang.isPresent()) {
      return BrukerinformasjonResponse.builder().feilkodeTilgang(feilkodeTilgang).forelderrolle(brukersForelderrolle)
          .kanOppretteFarskapserklaering(false).gyldigForelderrolle(false).build();
    }

    if (Forelderrolle.MOR.equals(brukersForelderrolle) || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) {

      // Vurdere om sivilstand kvalifiserer til at mor kan bruke løsningen
      feilkodeTilgang = getFeilkode(fnrPaaloggetBruker);
      kanOppretteFarskapserklaering = feilkodeTilgang.isEmpty();

      // har mor noen nyfødte barn uten registrert far?
      nyligFoedteBarnSomManglerFar = personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrPaaloggetBruker);

      var alleMorsAktiveErklaeringer = persistenceService.henteMorsEksisterendeErklaeringer(fnrPaaloggetBruker);

      // Erklæringer som mangler mors signatur
      avventerSignereringPaaloggetBruker = alleMorsAktiveErklaeringer.stream().filter(Objects::nonNull)
          .filter(fe -> fe.getDokument().getSignertAvMor() == null).collect(Collectors.toSet());

      // Hente mors erklæringer som bare mangler fars signatur
      avventerSigneringMotpart = alleMorsAktiveErklaeringer.stream().filter(Objects::nonNull).filter(fe -> fe.getDokument().getSignertAvMor() != null)
          .filter(fe -> fe.getDokument().getSignertAvFar() == null).collect(Collectors.toSet());

      // Mors erklaeringer som er signert av begge foreldrene
      avventerRegistreringSkatt = alleMorsAktiveErklaeringer.stream().filter(Objects::nonNull)
          .filter(fe -> fe.getDokument().getSignertAvMor() != null).filter(fe -> fe.getDokument().getSignertAvFar() != null)
          .collect(Collectors.toSet());
    }

    if (Forelderrolle.FAR.equals(brukersForelderrolle) || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) {

      var farsErklaeringer = persistenceService.henteFarsErklaeringer(fnrPaaloggetBruker);

      // Mangler fars signatur
      avventerSignereringPaaloggetBruker.addAll(
          farsErklaeringer.stream().filter(Objects::nonNull).filter(fe -> null == fe.getDokument().getSignertAvFar()).collect(Collectors.toSet()));

      // Avventer registrering hos Skatt. For rolle MOR_ELLER_FAR kan lista allerede inneholde innslag for mor
      avventerRegistreringSkatt.addAll(
          farsErklaeringer.stream().filter(Objects::nonNull).filter(fe -> null != fe.getDokument().getSignertAvFar()).collect(Collectors.toSet()));

    }

    return BrukerinformasjonResponse.builder().forelderrolle(brukersForelderrolle).avventerSigneringMotpart(avventerSigneringMotpart)
        .fnrNyligFoedteBarnUtenRegistrertFar(nyligFoedteBarnSomManglerFar).gyldigForelderrolle(true)
        .kanOppretteFarskapserklaering(kanOppretteFarskapserklaering).avventerSigneringBruker(avventerSignereringPaaloggetBruker)
        .avventerRegistrering(avventerRegistreringSkatt).feilkodeTilgang(feilkodeTilgang).build();
  }


  private Optional<Feilkode> vurdereTilgangBasertPaaAlderOgForeldrerolle(String foedselsnummer, Forelderrolle forelderrolle) {

    // Kun myndige personer kan bruke løsningen
    if (!erMyndig(foedselsnummer)) {
      return Optional.of(Feilkode.IKKE_MYNDIG);
      // Løsningen er ikke åpen for medmor eller person med udefinerbar forelderrolle
    } else if (Forelderrolle.MEDMOR.equals(forelderrolle) || Forelderrolle.UKJENT.equals(forelderrolle)) {
      return Optional.of(Feilkode.MEDMOR_ELLER_UKJENT);
    }
    return Optional.empty();
  }

  private boolean erMyndig(String foedselsnummer) {
    var foedselsdato = personopplysningService.henteFoedselsdato(foedselsnummer);
    return LocalDate.now().minusYears(18).isAfter(foedselsdato.minusDays(1));
  }

  private Optional<Feilkode> getFeilkode(String foedselsnummer) {
    var sivilstand = personopplysningService.henteSivilstand(foedselsnummer);
    return switch (sivilstand.getType()) {
      case GIFT -> Optional.of(Feilkode.MOR_SIVILSTAND_GIFT);
      case REGISTRERT_PARTNER -> Optional.of(Feilkode.MOR_SIVILSTAND_REGISTRERT_PARTNER);
      case UOPPGITT -> Optional.of(Feilkode.MOR_SIVILSTAND_UOPPGITT);
      default -> Optional.empty();
    };
  }

  private void riktigRolleForOpprettingAvErklaering(String fnrPaaloggetPerson) {
    log.info("Sjekker om person kan opprette farskapserklaering..");

    var feilkode = getFeilkode(fnrPaaloggetPerson);

    var kjoennPaaloggetPerson = personopplysningService.bestemmeForelderrolle(fnrPaaloggetPerson);
    var paaloggetPersonKanOpptreSomMor =
        feilkode.isEmpty() && (Forelderrolle.MOR.equals(kjoennPaaloggetPerson) || Forelderrolle.MOR_ELLER_FAR.equals(kjoennPaaloggetPerson));

    if (!paaloggetPersonKanOpptreSomMor) {
      throw new OppretteFarskapserklaeringException(feilkode.isEmpty() ? Feilkode.FEIL_ROLLE_OPPRETTE : feilkode.get());
    }
  }

  public OppretteFarskapserklaeringResponse oppretteFarskapserklaering(String fnrMor, OppretteFarskaperklaeringRequest request) {
    // Sjekker om mor skal kunne opprette ny farskapserklæring
    validereTilgang(fnrMor, request);

    var barn = BarnDto.builder().termindato(request.getBarn().getTermindato()).build();
    if (request.getBarn().getFoedselsnummer() != null && !request.getBarn().getFoedselsnummer().isBlank()) {
      barn.setFoedselsnummer(request.getBarn().getFoedselsnummer());
    }

    var navnMor = personopplysningService.henteNavn(fnrMor);
    var navnFar = personopplysningService.henteNavn(request.getOpplysningerOmFar().getFoedselsnummer());

    var mor = ForelderDto.builder().foedselsnummer(fnrMor).fornavn(navnMor.getFornavn()).mellomnavn(navnMor.getMellomnavn())
        .etternavn(navnMor.getEtternavn()).build();

    var far = ForelderDto.builder().forelderrolle(Forelderrolle.FAR).foedselsnummer(request.getOpplysningerOmFar().getFoedselsnummer())
        .fornavn(navnFar.getFornavn()).mellomnavn(navnFar.getMellomnavn()).etternavn(navnFar.getEtternavn()).build();

    var farskapserklaeringDto = FarskapserklaeringDto.builder().barn(barn).mor(mor).far(far).build();
    var dokumentDto = pdfGeneratorConsumer.genererePdf(farskapserklaeringDto);

    // Opprette signeringsjobb, oppdaterer dokument med status-url og redirect-url-ers
    difiESignaturConsumer.oppretteSigneringsjobb(dokumentDto, mor, far);
    farskapserklaeringDto.setDokument(dokumentDto);

    log.info("Lagre farskapserklæring");
    persistenceService.lagreFarskapserklaering(farskapserklaeringDto);

    return OppretteFarskapserklaeringResponse.builder().redirectUrlForSigneringMor(dokumentDto.getRedirectUrlMor().toString()).build();
  }

  private void validereTilgang(String fnrMor, OppretteFarskaperklaeringRequest request) {
    // Mor må være myndig
    Validate.isTrue(erMyndig(fnrMor), "Mor kan ikke bruke løsningen dersom hun ikke er myndig");
    // Bare mor kan oppretteFarskapserklæring
    riktigRolleForOpprettingAvErklaering(fnrMor);
    // Kontrollere opplysninger om far i request
    personopplysningService.riktigNavnRolleFar(request.getOpplysningerOmFar().getFoedselsnummer(), request.getOpplysningerOmFar().getNavn());
    // Far må være myndig
    Validate.isTrue(erMyndig(request.getOpplysningerOmFar().getFoedselsnummer()), "Far må være myndig");
    // Kontrollere at evnt nyfødt barn uten far er registrert med relasjon til mor
    validereRelasjonerNyfoedt(fnrMor, request.getBarn().getFoedselsnummer());
    // Validere alder på nyfødt
    validereAlderNyfoedt(request.getBarn().getFoedselsnummer());
    // Kontrollere at mor og far ikke er samme person
    Validate
        .isTrue(morOgFarErForskjelligePersoner(fnrMor, request.getOpplysningerOmFar().getFoedselsnummer()), "Mor og far kan ikke være samme person!");
    // Validere at termindato er innenfor gyldig intervall dersom barn ikke er født
    Validate.isTrue(termindatoErGyldig(request.getBarn()), "Termindato er ikke innenfor gyldig intervall!");
    // Sjekke at ny farskapserklæring ikke kommmer i konflikt med eksisterende
    persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(fnrMor,
        BarnDto.builder().termindato(request.getBarn().getTermindato()).foedselsnummer(request.getBarn().getFoedselsnummer()).build());
  }


  private void validereAlderNyfoedt(String fnrOppgittBarn) {
    if (fnrOppgittBarn == null || fnrOppgittBarn.length() < 1) {
      return;
    }
    var foedselsdato = personopplysningService.henteFoedselsdato(fnrOppgittBarn);
    if (!LocalDate.now().minusMonths(farskapsportalEgenskaper.getMaksAntallMaanederEtterFoedsel()).isBefore(foedselsdato)) {
      throw new NyfoedtErForGammelException(Feilkode.NYFODT_ER_FOR_GAMMEL);
    }
  }

  private void validereRelasjonerNyfoedt(String fnrMor, String fnrOppgittBarn) {

    if (fnrOppgittBarn == null || fnrOppgittBarn.length() < 1) {
      log.info("Barnet er ikke oppgitt med fødselsnummer");
      return;
    }

    log.info("Validerer at nyfødt barn er relatert til mor, samt har ingen registrert far.");
    var registrerteNyfoedteUtenFar = personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

    registrerteNyfoedteUtenFar.stream().findFirst().orElseThrow(() -> new MorHarIngenNyfoedteUtenFarException(Feilkode.INGEN_NYFOEDTE_UTEN_FAR));

    registrerteNyfoedteUtenFar.stream().filter(Objects::nonNull).filter(fnrBarn -> fnrBarn.equals(fnrOppgittBarn)).collect(Collectors.toSet())
        .stream().findAny().orElseThrow(() -> new ManglerRelasjonException(Feilkode.BARN_MANGLER_RELASJON_TIL_MOR));
  }

  /**
   * Henter signert dokument. Lagrer pades-url for fremtidige dokument-nedlastinger (Transactional)
   *
   * @param fnrPaaloggetPerson fødselsnummer til pålogget person
   * @param statusQueryToken tilgangstoken fra e-signeringsløsningen
   * @return kopi av signert dokument
   */
  @Transactional
  public byte[] henteSignertDokumentEtterRedirect(String fnrPaaloggetPerson, String statusQueryToken) {

    // Forelder må være myndig

    Validate.isTrue(erMyndig(fnrPaaloggetPerson), "Person må være myndig for å bruke løsningen");

    var farskapserklaeringer = henteFarskapserklaeringerEtterRedirect(fnrPaaloggetPerson);

    if (farskapserklaeringer.size() < 1) {
      throw new FarskapserklaeringIkkeFunnetException("Fant ingen påbegynt farskapserklæring for pålogget bruker");
    }

    // Henter dokument fra Postens signeringstjeneste
    var farskapserklaeringDtoSet = farskapserklaeringer.stream().map(fe -> mappingUtil.toDto(fe)).collect(Collectors.toSet());
    var dokumentStatusDto = henteDokumentstatusEtterRedirect(statusQueryToken, farskapserklaeringDtoSet);

    // filtrerer ut farskapserklæringen statuslenka tilhører
    var aktuellFarskapserklaering = farskapserklaeringer.stream().filter(Objects::nonNull)
        .filter(fe -> fe.getDokument().getDokumentStatusUrl().equals(dokumentStatusDto.getStatuslenke().toString())).collect(Collectors.toSet())
        .stream().findAny().orElseThrow(() -> new FarskapserklaeringIkkeFunnetException("Fant ikke farskapserklæring"));

    // oppdatere padeslenke i aktuell farskapserklæring
    aktuellFarskapserklaering.getDokument().setPadesUrl(dokumentStatusDto.getPadeslenke().toString());

    // Oppdatere foreldrenes signeringsstatus
    for (SignaturDto signatur : dokumentStatusDto.getSignaturer()) {
      if (aktuellFarskapserklaering.getMor().getFoedselsnummer().equals(signatur.getSignatureier())) {
        aktuellFarskapserklaering.getDokument().setSignertAvMor(signatur.getTidspunktForSignering());
      } else if (aktuellFarskapserklaering.getFar().getFoedselsnummer().equals(signatur.getSignatureier())) {
        aktuellFarskapserklaering.getDokument().setSignertAvFar(signatur.getTidspunktForSignering());
      } else {
        throw new HentingAvDokumentFeiletException("Dokumentets signatureiere er forskjellige fra partene som er registrert i farskapserklæringen!");
      }
    }

    // returnerer kopi av signert dokument
    return difiESignaturConsumer.henteSignertDokument(dokumentStatusDto.getPadeslenke());
  }

  private boolean morOgFarErForskjelligePersoner(String fnrMor, String fnrFar) {
    log.info("Sjekker at mor og far ikke er én og samme person");
    return !fnrMor.equals(fnrFar);
  }

  private boolean termindatoErGyldig(BarnDto barnDto) {
    log.info("Validerer termindato");
    if (barnDto.getFoedselsnummer() != null && !barnDto.getFoedselsnummer().isBlank() && barnDto.getFoedselsnummer().length() > 10) {
      log.info("Termindato er ikke oppgitt");
      return true;
    } else {
      var nedreGrense = LocalDate.now().plusWeeks(farskapsportalEgenskaper.getMinAntallUkerTilTermindato() - 1);
      var oevreGrense = LocalDate.now().plusWeeks(farskapsportalEgenskaper.getMaksAntallUkerTilTermindato() + 1);
      return nedreGrense.isBefore(barnDto.getTermindato()) && oevreGrense.isAfter(barnDto.getTermindato());
    }
  }

  @Transactional
  Set<Farskapserklaering> henteFarskapserklaeringerEtterRedirect(String fnrPaaloggetPerson) {

    var brukersForelderrolle = personopplysningService.bestemmeForelderrolle(fnrPaaloggetPerson);
    var gjeldendeKjoenn = personopplysningService.henteGjeldendeKjoenn(fnrPaaloggetPerson);

    if ((Forelderrolle.MOR.equals(brukersForelderrolle) || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) && KjoennType.KVINNE
        .equals(gjeldendeKjoenn.getKjoenn())) {
      return persistenceService.henteFarskapserklaeringerEtterRedirect(fnrPaaloggetPerson, brukersForelderrolle, gjeldendeKjoenn.getKjoenn());

    } else if ((Forelderrolle.FAR.equals(brukersForelderrolle)) || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle) && KjoennType.MANN
        .equals(gjeldendeKjoenn.getKjoenn())) {
      return persistenceService.henteFarskapserklaeringerEtterRedirect(fnrPaaloggetPerson, brukersForelderrolle, gjeldendeKjoenn.getKjoenn());
    }

    throw new PersonHarFeilRolleException("Pålogget person kan verken opptre som mor eller far i løsningen!");
  }

  private DokumentStatusDto henteDokumentstatusEtterRedirect(String statusQueryToken, Set<FarskapserklaeringDto> farskapserklaeringer) {

    Set<URI> dokumentStatuslenker = farskapserklaeringer.stream().map(FarskapserklaeringDto::getDokument).map(DokumentDto::getDokumentStatusUrl)
        .collect(Collectors.toSet());

    // Mangler sikker identifisering av hvilken statuslenke tokenet er tilknyuttet. Forelder kan
    // potensielt ha flere farskapserklæringer som er startet men hvor signeringsprosessen ikke
    // er fullført. Returnerer statuslenke som hører til statusQueryToken.
    return difiESignaturConsumer.henteDokumentstatusEtterRedirect(statusQueryToken, dokumentStatuslenker);
  }
}
