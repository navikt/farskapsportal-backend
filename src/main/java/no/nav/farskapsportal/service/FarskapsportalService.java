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
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppretteFarskaperklaeringRequest;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringResponse;
import no.nav.farskapsportal.config.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.DokumentStatusDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.SignaturDto;
import no.nav.farskapsportal.exception.FarskapserklaeringIkkeFunnetException;
import no.nav.farskapsportal.exception.FeilNavnOppgittException;
import no.nav.farskapsportal.exception.HentingAvDokumentFeiletException;
import no.nav.farskapsportal.exception.ManglerRelasjonException;
import no.nav.farskapsportal.exception.MorHarIngenNyfoedteUtenFarException;
import no.nav.farskapsportal.exception.NyfoedtErForGammelException;
import no.nav.farskapsportal.exception.ValideringException;
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

    // Avbryte videre flyt dersom bruker ikke er myndig eller har en rolle som ikke støttes av løsningen
    validereTilgangBasertPaaAlderOgForeldrerolle(fnrPaaloggetBruker, brukersForelderrolle);

    if (Forelderrolle.MOR.equals(brukersForelderrolle) || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) {

      // Vurdere om sivilstand kvalifiserer til at mor kan bruke løsningen
      validereSivilstand(fnrPaaloggetBruker);

      kanOppretteFarskapserklaering = true;

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
        .avventerRegistrering(avventerRegistreringSkatt).build();
  }


  private void validereTilgangBasertPaaAlderOgForeldrerolle(String foedselsnummer, Forelderrolle forelderrolle) {
    // Kun myndige personer kan bruke løsningen
    personopplysningService.erMyndig(foedselsnummer);

    // Løsningen er ikke åpen for medmor eller person med udefinerbar forelderrolle
    if (Forelderrolle.MEDMOR.equals(forelderrolle) || Forelderrolle.UKJENT.equals(forelderrolle)) {
      throw new ValideringException(Feilkode.MEDMOR_ELLER_UKJENT);
    }
  }

  private void validereSivilstand(String foedselsnummer) {
    var sivilstand = personopplysningService.henteSivilstand(foedselsnummer);
    switch (sivilstand.getType()) {
      case GIFT -> throw new ValideringException(Feilkode.MOR_SIVILSTAND_GIFT);
      case REGISTRERT_PARTNER -> throw new ValideringException(Feilkode.MOR_SIVILSTAND_REGISTRERT_PARTNER);
      case UOPPGITT -> throw new ValideringException(Feilkode.MOR_SIVILSTAND_UOPPGITT);
    }
  }

  private void riktigRolleForOpprettingAvErklaering(String fnrPaaloggetPerson) {
    log.info("Sjekker om person kan opprette farskapserklaering..");

    validereSivilstand(fnrPaaloggetPerson);

    var kjoennPaaloggetPerson = personopplysningService.bestemmeForelderrolle(fnrPaaloggetPerson);
    var paaloggetPersonKanOpptreSomMor = Forelderrolle.MOR.equals(kjoennPaaloggetPerson) || Forelderrolle.MOR_ELLER_FAR.equals(kjoennPaaloggetPerson);

    if (!paaloggetPersonKanOpptreSomMor) {
      throw new ValideringException(Feilkode.FEIL_ROLLE_OPPRETTE);
    }
  }

  @Transactional
  public OppretteFarskapserklaeringResponse oppretteFarskapserklaering(String fnrMor, OppretteFarskaperklaeringRequest request) {
    // Sjekker om mor skal kunne opprette ny farskapserklæring
    validereTilgangMor(fnrMor, request);
    // Sjekker om mor har oppgitt riktige opplysninger om far, samt at far tilfredsstiller krav til digital erklæering
    personopplysningService.riktigNavnRolleFar(request.getOpplysningerOmFar().getFoedselsnummer(), request.getOpplysningerOmFar().getNavn());

    var barn = BarnDto.builder().termindato(request.getBarn().getTermindato()).build();
    if (request.getBarn().getFoedselsnummer() != null && !request.getBarn().getFoedselsnummer().isBlank()) {
      barn.setFoedselsnummer(request.getBarn().getFoedselsnummer());
    }

    var mor = getForelderDto(fnrMor, null);
    var far = getForelderDto(request.getOpplysningerOmFar().getFoedselsnummer(), Forelderrolle.FAR);

    var farskapserklaering = Farskapserklaering.builder().barn(mappingUtil.toEntity(barn)).mor(mappingUtil.toEntity(mor))
        .far(mappingUtil.toEntity(far)).build();
    var dokument = pdfGeneratorConsumer.genererePdf(farskapserklaering);

    // Opprette signeringsjobb, oppdaterer dokument med status-url og redirect-url-ers
    difiESignaturConsumer.oppretteSigneringsjobb(dokument, mappingUtil.toEntity(mor), mappingUtil.toEntity(far));
    farskapserklaering.setDokument(dokument);

    log.info("Lagre farskapserklæring");
    persistenceService.lagreNyFarskapserklaering(farskapserklaering);

    return OppretteFarskapserklaeringResponse.builder().redirectUrlForSigneringMor(dokument.getSigneringsinformasjonMor().getRedirectUrl()).build();
  }

  private ForelderDto getForelderDto(String fnr, Forelderrolle rolle) {
    var navn = personopplysningService.henteNavn(fnr);
    return ForelderDto.builder().forelderrolle(rolle).foedselsnummer(fnr).fornavn(navn.getFornavn()).mellomnavn(navn.getMellomnavn())
        .etternavn(navn.getEtternavn()).build();
  }

  public void kontrollereFar(String fnrMor, KontrollerePersonopplysningerRequest request) {
    antallsbegrensetKontrollAvNavnOgNummerPaaFar(fnrMor, request);
    validereRolleOgAlderFar(request.getFoedselsnummer());
  }

  private void antallsbegrensetKontrollAvNavnOgNummerPaaFar(String fnrMor, KontrollerePersonopplysningerRequest request) {
    var statusKontrollereFar = persistenceService.henteStatusKontrollereFar(fnrMor);
    if (statusKontrollereFar.isEmpty() || farskapsportalEgenskaper.getMaksAntallForsoek() > statusKontrollereFar.get().getAntallFeiledeForsoek()) {
      kontrollereNavnOgNummerFar(fnrMor, request);
    } else {
      throw new ValideringException(Feilkode.MAKS_ANTALL_FORSOEK);
    }
  }

  private void kontrollereNavnOgNummerFar(String fnrMor, KontrollerePersonopplysningerRequest request) {
    try {
      validereOppgittNavnFar(request.getFoedselsnummer(), request.getNavn());
    } catch (FeilNavnOppgittException e) {
      var statusKontrollereFarDto = mappingUtil
          .toDto(persistenceService.oppdatereStatusKontrollereFar(fnrMor, farskapsportalEgenskaper.getForsoekFornyesEtterAntallDager()));
      e.setStatusKontrollereFarDto(Optional.of(statusKontrollereFarDto));
      var resterendeAntallForsoek = farskapsportalEgenskaper.getMaksAntallForsoek() - statusKontrollereFarDto.getAntallFeiledeForsoek();
      statusKontrollereFarDto.setAntallResterendeForsoek(resterendeAntallForsoek);
      throw e;
    }
  }

  private void validereOppgittNavnFar(String foedselsnummerFar, String fulltNavnFar) {
    var feilkode = foedselsnummerFar == null || foedselsnummerFar.trim().length() < 1 ? Optional.of(Feilkode.FOEDSELNUMMER_MANGLER_FAR)
        : Optional.<Feilkode>empty();
    feilkode = fulltNavnFar == null || fulltNavnFar.trim().length() < 1 ? Optional.of(Feilkode.FOEDSELNUMMER_MANGLER_FAR) : feilkode;
    if (feilkode.isPresent()) {
      throw new ValideringException(feilkode.get());
    }

    NavnDto navnDtoFraFolkeregisteret = personopplysningService.henteNavn(foedselsnummerFar);

    // Validere input
    personopplysningService.navnekontroll(fulltNavnFar, navnDtoFraFolkeregisteret);
  }

  private void validereRolleOgAlderFar(String foedselsnummer) {
    if (!Forelderrolle.FAR.equals(personopplysningService.bestemmeForelderrolle(foedselsnummer))) {
      throw new ValideringException(Feilkode.FEIL_ROLLE_FAR);
    }

    // Far må være myndig
    personopplysningService.erMyndig(foedselsnummer);
  }

  public void validereMor(String fnrMor) {
    // Mor må være myndig
    personopplysningService.erMyndig(fnrMor);
    // Bare mor kan oppretteFarskapserklæring
    riktigRolleForOpprettingAvErklaering(fnrMor);
  }

  private void validereTilgangMor(String fnrMor, OppretteFarskaperklaeringRequest request) {
    validereMor(fnrMor);
    // Kontrollere at evnt nyfødt barn uten far er registrert med relasjon til mor
    validereRelasjonerNyfoedt(fnrMor, request.getBarn().getFoedselsnummer());
    // Validere alder på nyfødt
    validereAlderNyfoedt(request.getBarn().getFoedselsnummer());
    // Kontrollere at mor og far ikke er samme person
    Validate
        .isTrue(morOgFarErForskjelligePersoner(fnrMor, request.getOpplysningerOmFar().getFoedselsnummer()), "Mor og far kan ikke være samme person!");
    // Validere at termindato er innenfor gyldig intervall dersom barn ikke er født
    termindatoErGyldig(request.getBarn());
    // Sjekke at ny farskapserklæring ikke kommmer i konflikt med eksisterende
    persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(fnrMor, request.getOpplysningerOmFar().getFoedselsnummer(),
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
  public FarskapserklaeringDto henteSignertDokumentEtterRedirect(String fnrPaaloggetPerson, String statusQueryToken) {

    // Forelder må være myndig
    personopplysningService.erMyndig(fnrPaaloggetPerson);

    var farskapserklaeringer = henteFarskapserklaeringerEtterRedirect(fnrPaaloggetPerson);

    if (farskapserklaeringer.size() < 1) {
      throw new FarskapserklaeringIkkeFunnetException("Fant ingen påbegynt farskapserklæring for pålogget bruker");
    }

    // Henter dokument fra Postens signeringstjeneste
    var dokumentStatusDto = henteDokumentstatusEtterRedirect(statusQueryToken,
        farskapserklaeringer.stream().map(fe -> mappingUtil.toDto(fe)).collect(Collectors.toSet()));

    // filtrerer ut farskapserklæringen statuslenka tilhører
    var aktuellFarskapserklaering = farskapserklaeringer.stream().filter(Objects::nonNull)
        .filter(fe -> fe.getDokument().getDokumentStatusUrl().equals(dokumentStatusDto.getStatuslenke().toString())).collect(Collectors.toSet())
        .stream().findAny().orElseThrow(() -> new FarskapserklaeringIkkeFunnetException("Fant ikke farskapserklæring"));

    // oppdatere padeslenke i aktuell farskapserklæring
    aktuellFarskapserklaering.getDokument().setPadesUrl(dokumentStatusDto.getPadeslenke().toString());

    // Oppdatere foreldrenes signeringsstatus
    for (SignaturDto signatur : dokumentStatusDto.getSignaturer()) {
      if (aktuellFarskapserklaering.getMor().getFoedselsnummer().equals(signatur.getSignatureier())) {
        aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(signatur.getTidspunktForSignering());
      } else if (aktuellFarskapserklaering.getFar().getFoedselsnummer().equals(signatur.getSignatureier())) {
        aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(signatur.getTidspunktForSignering());
      } else {
        throw new HentingAvDokumentFeiletException("Dokumentets signatureiere er forskjellige fra partene som er registrert i farskapserklæringen!");
      }
    }

    // Oppdaterer dokumentinnhold
    var signertDokument = difiESignaturConsumer.henteSignertDokument(dokumentStatusDto.getPadeslenke());
    aktuellFarskapserklaering.getDokument().setInnhold(signertDokument);

    return mappingUtil.toDto(aktuellFarskapserklaering);
  }

  private boolean morOgFarErForskjelligePersoner(String fnrMor, String fnrFar) {
    log.info("Sjekker at mor og far ikke er én og samme person");
    return !fnrMor.equals(fnrFar);
  }

  private void termindatoErGyldig(BarnDto barnDto) {
    log.info("Validerer termindato");

    if (barnDto.getFoedselsnummer() != null && !barnDto.getFoedselsnummer().isBlank() && barnDto.getFoedselsnummer().length() > 10) {
      log.info("Termindato er ikke oppgitt");
      return;
    } else {
      var nedreGrense = LocalDate.now().plusWeeks(farskapsportalEgenskaper.getMinAntallUkerTilTermindato() - 1);
      var oevreGrense = LocalDate.now().plusWeeks(farskapsportalEgenskaper.getMaksAntallUkerTilTermindato() + 1);
      if (nedreGrense.isBefore(barnDto.getTermindato()) && oevreGrense.isAfter(barnDto.getTermindato())) {
        log.info("Termindato validert");
        return;
      }
    }

    throw new ValideringException(Feilkode.TERMINDATO_UGYLDIG);
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

    throw new ValideringException(Feilkode.FEIL_ROLLE);
  }

  private DokumentStatusDto henteDokumentstatusEtterRedirect(String statusQueryToken, Set<FarskapserklaeringDto> farskapserklaeringDtos) {

    Set<URI> dokumentStatuslenker = farskapserklaeringDtos.stream().map(FarskapserklaeringDto::getDokument).map(DokumentDto::getDokumentStatusUrl)
        .collect(Collectors.toSet());

    // Mangler sikker identifisering av hvilken statuslenke tokenet er tilknyuttet. Forelder kan
    // potensielt ha flere farskapserklæringer som er startet men hvor signeringsprosessen ikke
    // er fullført. Returnerer statuslenke som hører til statusQueryToken.
    return difiESignaturConsumer.henteDokumentstatusEtterRedirect(statusQueryToken, dokumentStatuslenker);
  }

  public URI henteNyRedirectUrl(String fnrPaaloggetPerson, int idFarskapserklaering) {
    var undertegnerUrl = persistenceService.henteUndertegnerUrl(fnrPaaloggetPerson, idFarskapserklaering);
    return difiESignaturConsumer.henteNyRedirectUrl(undertegnerUrl);
  }
}
