package no.nav.farskapsportal.service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
import javax.transaction.Transactional;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.BrukerinformasjonResponse;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppdatereFarskapserklaeringRequest;
import no.nav.farskapsportal.api.OppdatereFarskapserklaeringResponse;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringRequest;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringResponse;
import no.nav.farskapsportal.api.Rolle;
import no.nav.farskapsportal.config.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.EsigneringConsumerException;
import no.nav.farskapsportal.exception.FeilNavnOppgittException;
import no.nav.farskapsportal.exception.InternFeilException;
import no.nav.farskapsportal.exception.ManglerRelasjonException;
import no.nav.farskapsportal.exception.MappingException;
import no.nav.farskapsportal.exception.MorHarIngenNyfoedteUtenFarException;
import no.nav.farskapsportal.exception.NyfoedtErForGammelException;
import no.nav.farskapsportal.exception.SkattConsumerException;
import no.nav.farskapsportal.exception.ValideringException;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.util.Mapper;
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
  private final SkattConsumer skattConsumer;
  private final PersistenceService persistenceService;
  private final PersonopplysningService personopplysningService;
  private final Mapper mapper;

  private static long getUnikId(byte[] dokument, LocalDateTime tidspunktForSignering) {
    var crc32 = new CRC32();
    var outputstream = new ByteArrayOutputStream();
    outputstream.writeBytes(dokument);
    outputstream.writeBytes(tidspunktForSignering.toString().getBytes());
    crc32.update(outputstream.toByteArray());

    return crc32.getValue();
  }

  public BrukerinformasjonResponse henteBrukerinformasjon(String fnrPaaloggetBruker) {

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
      avventerSignereringPaaloggetBruker.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.MOR));

      // Hente mors erklæringer som bare mangler fars signatur
      avventerSigneringMotpart = alleMorsAktiveErklaeringer.stream().filter(Objects::nonNull).filter(fe -> fe.getDokument().getSignertAvMor() != null)
          .filter(fe -> fe.getDokument().getSignertAvFar() == null).collect(Collectors.toSet());
      avventerSigneringMotpart.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.MOR));

      // Mors erklaeringer som er signert av begge foreldrene
      avventerRegistreringSkatt = alleMorsAktiveErklaeringer.stream().filter(Objects::nonNull)
          .filter(fe -> fe.getDokument().getSignertAvMor() != null).filter(fe -> fe.getDokument().getSignertAvFar() != null)
          .collect(Collectors.toSet());
      avventerRegistreringSkatt.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.MOR));
    }

    if (Forelderrolle.FAR.equals(brukersForelderrolle) || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) {

      var farsErklaeringer = persistenceService.henteFarsErklaeringer(fnrPaaloggetBruker);

      // Mangler fars signatur
      avventerSignereringPaaloggetBruker.addAll(
          farsErklaeringer.stream().filter(Objects::nonNull).filter(fe -> null == fe.getDokument().getSignertAvFar()).collect(Collectors.toSet()));
      avventerSignereringPaaloggetBruker.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.FAR));

      // Avventer registrering hos Skatt. For rolle MOR_ELLER_FAR kan lista allerede inneholde innslag for mor
      avventerRegistreringSkatt.addAll(
          farsErklaeringer.stream().filter(Objects::nonNull).filter(fe -> null != fe.getDokument().getSignertAvFar()).collect(Collectors.toSet()));
      avventerRegistreringSkatt.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.FAR));
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
  public OppretteFarskapserklaeringResponse oppretteFarskapserklaering(String fnrMor, OppretteFarskapserklaeringRequest request) {

    // Sjekker om mor skal kunne opprette ny farskapserklæring
    validereTilgangMor(fnrMor, request);

    // Sjekker om mor har oppgitt riktige opplysninger om far, samt at far tilfredsstiller krav til digital erklæering
    kontrollereNavnOgNummerFar(fnrMor, request.getOpplysningerOmFar());
    validereFar(request.getOpplysningerOmFar().getFoedselsnummer());

    var barn = BarnDto.builder().termindato(request.getBarn().getTermindato()).build();
    if (request.getBarn().getFoedselsnummer() != null && !request.getBarn().getFoedselsnummer().isBlank()) {
      barn.setFoedselsnummer(request.getBarn().getFoedselsnummer());
    }

    var mor = getForelderDto(fnrMor, null);
    var far = getForelderDto(request.getOpplysningerOmFar().getFoedselsnummer(), Forelderrolle.FAR);

    var farskapserklaering = Farskapserklaering.builder()
        .barn(mapper.toEntity(barn))
        .mor(mapper.toEntity(mor))
        .far(mapper.toEntity(far))
        .morBorSammenMedFar(request.isMorBorSammenMedFar())
        .build();
    var dokument = pdfGeneratorConsumer.genererePdf(farskapserklaering);

    // Opprette signeringsjobb, oppdaterer dokument med status-url og redirect-url-ers
    difiESignaturConsumer.oppretteSigneringsjobb(dokument, mapper.toEntity(mor), mapper.toEntity(far));
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
    validereFar(request.getFoedselsnummer());
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
      var statusKontrollereFarDto = mapper
          .toDto(persistenceService.oppdatereStatusKontrollereFar(fnrMor, farskapsportalEgenskaper.getForsoekFornyesEtterAntallDager()));
      e.setStatusKontrollereFarDto(Optional.of(statusKontrollereFarDto));
      var resterendeAntallForsoek = farskapsportalEgenskaper.getMaksAntallForsoek() - statusKontrollereFarDto.getAntallFeiledeForsoek();
      statusKontrollereFarDto.setAntallResterendeForsoek(resterendeAntallForsoek);
      throw e;
    }
  }

  private void validereOppgittNavnFar(String foedselsnummerFar, String fulltNavnFar) {

    if (foedselsnummerFar == null || foedselsnummerFar.trim().length() < 1) {
      throw new ValideringException(Feilkode.FOEDSELNUMMER_MANGLER_FAR);
    }

    if (fulltNavnFar == null || fulltNavnFar.trim().length() < 1) {
      throw new ValideringException(Feilkode.KONTROLLERE_FAR_NAVN_MANGLER);
    }

    NavnDto navnDtoFraFolkeregisteret = personopplysningService.henteNavn(foedselsnummerFar);

    // Validere input
    personopplysningService.navnekontroll(fulltNavnFar, navnDtoFraFolkeregisteret);
  }

  private void validereFar(String foedselsnummer) {

    // Far må være myndig
    personopplysningService.erMyndig(foedselsnummer);

    var farsForelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

    // Far må ha foreldrerolle FAR eller MOR_ELLER_FAR
    if (!(Forelderrolle.FAR.equals(farsForelderrolle) || Forelderrolle.MOR_ELLER_FAR.equals(farsForelderrolle))) {
      throw new ValideringException(Feilkode.FEIL_ROLLE_FAR);
    }
  }

  public void validereMor(String fnrMor) {

    // Mor må være myndig
    personopplysningService.erMyndig(fnrMor);

    // Bare mor kan oppretteFarskapserklæring
    riktigRolleForOpprettingAvErklaering(fnrMor);
  }

  private void validereTilgangMor(String fnrMor, OppretteFarskapserklaeringRequest request) {
    // Validere alder og rolle
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
   * Oppdaterer status på signeringsjobb. Kalles etter at bruker har fullført signering. Lagrer pades-url for fremtidige dokument-nedlastinger
   * (Transactional)
   *
   * @param fnrPaaloggetPerson fødselsnummer til pålogget person
   * @param statusQueryToken tilgangstoken fra e-signeringsløsningen
   * @return kopi av signert dokument
   */
  @Transactional(dontRollbackOn = {SkattConsumerException.class})
  public FarskapserklaeringDto oppdatereStatus(String fnrPaaloggetPerson, String statusQueryToken) {

    // Forelder må være myndig
    personopplysningService.erMyndig(fnrPaaloggetPerson);

    var farskapserklaeringer = henteFarskapserklaeringerEtterRedirect(fnrPaaloggetPerson);

    if (farskapserklaeringer.size() < 1) {
      throw new ValideringException(Feilkode.PERSON_HAR_INGEN_VENTENDE_FARSKAPSERKLAERINGER);
    }

    // Henter status på signeringsjobben fra Postens signeringstjeneste
    var dokumentStatusDto = henteDokumentstatus(statusQueryToken, farskapserklaeringer);

    // filtrerer ut farskapserklæringen statuslenka tilhører
    var aktuellFarskapserklaering = farskapserklaeringer.stream().filter(Objects::nonNull)
        .filter(fe -> fe.getDokument().getDokumentStatusUrl().equals(dokumentStatusDto.getStatuslenke().toString())).collect(Collectors.toSet())
        .stream().findAny().orElseThrow(() -> new ValideringException(Feilkode.INGEN_TREFF_PAA_TOKEN));

    // Oppdatere foreldrenes signeringsinfo
    for (SignaturDto signatur : dokumentStatusDto.getSignaturer()) {
      if (fnrPaaloggetPerson.equals(aktuellFarskapserklaering.getMor().getFoedselsnummer()) && aktuellFarskapserklaering.getMor().getFoedselsnummer()
          .equals(signatur.getSignatureier())) {
        validereInnholdStatusrespons(dokumentStatusDto);
        aktuellFarskapserklaering.getDokument().setPadesUrl(dokumentStatusDto.getPadeslenke().toString());
        aktuellFarskapserklaering.getDokument().setBekreftelsesUrl(dokumentStatusDto.getBekreftelseslenke().toString());

        if (signatur.isHarSignert() && aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() == null) {
          aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(signatur.getTidspunktForStatus());
          var signertDokument = difiESignaturConsumer.henteSignertDokument(dokumentStatusDto.getPadeslenke());
          aktuellFarskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold(signertDokument).build());
          return mapper.toDto(aktuellFarskapserklaering);
        }

        if (aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null) {
          throw new ValideringException(Feilkode.PERSON_HAR_ALLEREDE_SIGNERT);
        } else {
          throw new ValideringException(Feilkode.SIGNERING_IKKE_GJENOMFOERT);
        }

      } else if (fnrPaaloggetPerson.equals(aktuellFarskapserklaering.getFar().getFoedselsnummer()) && aktuellFarskapserklaering.getFar()
          .getFoedselsnummer().equals(signatur.getSignatureier())) {
        validereInnholdStatusrespons(dokumentStatusDto);
        aktuellFarskapserklaering.getDokument().setBekreftelsesUrl(dokumentStatusDto.getBekreftelseslenke().toString());
        aktuellFarskapserklaering.getDokument().setPadesUrl(dokumentStatusDto.getPadeslenke().toString());
        if (aktuellFarskapserklaering.getSendtTilSkatt() == null && signatur.isHarSignert()) {
          aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(signatur.getTidspunktForStatus());
          var signertDokument = difiESignaturConsumer.henteSignertDokument(dokumentStatusDto.getPadeslenke());
          aktuellFarskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold(signertDokument).build());
          aktuellFarskapserklaering.setMeldingsidSkatt(getUnikId(aktuellFarskapserklaering.getDokument().getDokumentinnhold().getInnhold(),
              aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()));
        }
        return mapper.toDto(aktuellFarskapserklaering);
      }
    }

    throw new ValideringException(Feilkode.PERSON_IKKE_PART_I_FARSKAPSERKLAERING);
  }

  private void validereInnholdStatusrespons(DokumentStatusDto dokumentStatusDto) {
    try {
      Validate.isTrue(dokumentStatusDto.getStatuslenke() != null, "Statuslenke mangler");
      Validate.isTrue(dokumentStatusDto.getBekreftelseslenke() != null, "Bekreftelseslenke mangler");
      Validate.isTrue(dokumentStatusDto.getPadeslenke() != null, "Padeslenke mangler");
    } catch (IllegalArgumentException iae) {
      throw new EsigneringConsumerException("Manglende data retunert fra status-kall mot esigneringstjenesten", iae);
    }
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

  private Set<Farskapserklaering> henteFarskapserklaeringerEtterRedirect(String fnrPaaloggetPerson) {

    var brukersForelderrolle = personopplysningService.bestemmeForelderrolle(fnrPaaloggetPerson);

    var foreldersFarskapserklaeringer = persistenceService.henteFarskapserklaeringerForForelder(fnrPaaloggetPerson);

    if (Forelderrolle.MOR.equals(brukersForelderrolle)) {
      return foreldersFarskapserklaeringer.stream()
          .filter(Objects::nonNull)
          .filter(fe -> fe.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() == null)
          .collect(Collectors.toSet());
    } else if (Forelderrolle.FAR.equals(brukersForelderrolle)) {
      return foreldersFarskapserklaeringer.stream()
          .filter(Objects::nonNull)
          .filter(fe -> (fe.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null))
          .filter(fe -> (fe.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() == null))
          .collect(Collectors.toSet());
    } else if (Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) {
      // pålogget person kan potensielt være både mor og far i eksisterende farskapserklæringer
      var feMor = foreldersFarskapserklaeringer.stream()
          .filter(Objects::nonNull)
          .filter(fe -> fnrPaaloggetPerson.equals(fe.getMor().getFoedselsnummer()))
          .filter(fe -> (fe.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() == null))
          .collect(Collectors.toSet());
      var feFar = foreldersFarskapserklaeringer.stream().filter(Objects::nonNull)
          .filter(fe -> fnrPaaloggetPerson.equals(fe.getFar().getFoedselsnummer()))
          .filter(fe -> (fe.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null))
          .filter(fe -> (fe.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() == null))
          .collect(Collectors.toSet());

      feMor.addAll(feFar);
      return feMor;
    }

    throw new ValideringException(Feilkode.FEIL_ROLLE);
  }

  private DokumentStatusDto henteDokumentstatus(String statusQueryToken, Set<Farskapserklaering> farskapserklaeringer) {

    Set<URI> dokumentStatuslenker = farskapserklaeringer.stream().map(Farskapserklaering::getDokument).map(Dokument::getDokumentStatusUrl)
        .map(this::tilUri)
        .collect(Collectors.toSet());

    // Mangler sikker identifisering av hvilken statuslenke tokenet er tilknyuttet. Forelder kan
    // potensielt ha flere farskapserklæringer som er startet men hvor signeringsprosessen ikke
    // er fullført. Returnerer statuslenke som hører til statusQueryToken.
    return difiESignaturConsumer.henteStatus(statusQueryToken, dokumentStatuslenker);
  }

  private URI tilUri(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException urise) {
      throw new MappingException("Lagret status-URL har feil format", urise);
    }
  }

  @Transactional
  public URI henteNyRedirectUrl(String fnrPaaloggetPerson, int idFarskapserklaering) {
    var farskapserklaering = persistenceService.henteFarskapserklaeringForId(idFarskapserklaering);
    validereAtPersonErForelderIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering);
    validereAtPaaloggetPersonIkkeAlleredeHarSignert(fnrPaaloggetPerson, farskapserklaering);

    var undertegnerUrl = velgeRiktigUndertegnerUrl(fnrPaaloggetPerson, farskapserklaering);
    var nyRedirectUrl = difiESignaturConsumer.henteNyRedirectUrl(undertegnerUrl);

    if (personErMorIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering)) {
      farskapserklaering.getDokument().getSigneringsinformasjonMor().setRedirectUrl(nyRedirectUrl.toString());
    } else {
      farskapserklaering.getDokument().getSigneringsinformasjonFar().setRedirectUrl(nyRedirectUrl.toString());
    }

    return nyRedirectUrl;
  }

  private void validereAtPaaloggetPersonIkkeAlleredeHarSignert(String fnrPaaloggetPerson, Farskapserklaering farskapserklaering) {
    boolean erMor = personErMorIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering);
    if (erMor && farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() == null) {
      return;
    } else if (!erMor && farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() == null) {
      return;
    }
    throw new ValideringException(Feilkode.PERSON_HAR_ALLEREDE_SIGNERT);
  }

  private boolean morHarSignert(Farskapserklaering farskapserklaering) {
    return farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null;
  }

  private void validereAtPersonErForelderIFarskapserklaering(String foedselsnummer, Farskapserklaering farskapserklaering) {
    if (foedselsnummer.equals(farskapserklaering.getMor().getFoedselsnummer()) || foedselsnummer
        .equals(farskapserklaering.getFar().getFoedselsnummer())) {
      return;
    }
    throw new ValideringException(Feilkode.PERSON_IKKE_PART_I_FARSKAPSERKLAERING);
  }

  private boolean personErMorIFarskapserklaering(String foedselsnummer, Farskapserklaering farskapserklaering) {
    return foedselsnummer.equals(farskapserklaering.getMor().getFoedselsnummer());
  }

  private URI velgeRiktigUndertegnerUrl(String foedselsnummerUndertegner, Farskapserklaering farskapserklaering) {
    try {
      return new URI(foedselsnummerUndertegner.equals(farskapserklaering.getMor().getFoedselsnummer()) ? farskapserklaering.getDokument()
          .getSigneringsinformasjonMor().getUndertegnerUrl() : farskapserklaering.getDokument().getSigneringsinformasjonFar().getUndertegnerUrl());
    } catch (URISyntaxException e) {
      throw new InternFeilException(Feilkode.FEILFORMATERT_URL_UNDERTEGNERURL);
    }
  }

  @Transactional
  public OppdatereFarskapserklaeringResponse oppdatereFarskapserklaering(String fnrPaaloggetPerson, OppdatereFarskapserklaeringRequest request) {

    var farskapserklaering = persistenceService.henteFarskapserklaeringForId(request.getIdFarskapserklaering());
    validereAtPersonErForelderIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering);

    if (personErMorIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering)) {
      farskapserklaering.setMorBorSammenMedFar(request.isBorSammen());
    } else {
      farskapserklaering.setFarBorSammenMedMor(request.isBorSammen());
    }

    return OppdatereFarskapserklaeringResponse.builder().oppdatertFarskapserklaeringDto(mapper.toDto(farskapserklaering)).build();
  }

  public byte[] henteDokumentinnhold(String fnrForelder, int idFarskapserklaering) {
    var farskapserklaering = persistenceService.henteFarskapserklaeringForId(idFarskapserklaering);
    validereAtPersonErForelderIFarskapserklaering(fnrForelder, farskapserklaering);

    if (personErMorIFarskapserklaering(fnrForelder, farskapserklaering)) {
      return farskapserklaering.getDokument().getDokumentinnhold().getInnhold();
    } else if (morHarSignert(farskapserklaering)) {
      return farskapserklaering.getDokument().getDokumentinnhold().getInnhold();
    } else {
      throw new ValideringException(Feilkode.FARSKAPSERKLAERING_MANGLER_SIGNATUR_MOR);
    }
  }
}
