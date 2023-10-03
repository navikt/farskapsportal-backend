package no.nav.farskapsportal.backend.apps.api.service;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;
import static no.nav.farskapsportal.backend.libs.felles.util.Utils.getMeldingsidSkatt;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalApiEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.backend.apps.api.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.backend.apps.api.model.BrukerinformasjonResponse;
import no.nav.farskapsportal.backend.apps.api.model.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.backend.apps.api.model.OppdatereFarskapserklaeringRequest;
import no.nav.farskapsportal.backend.apps.api.model.OppdatereFarskapserklaeringResponse;
import no.nav.farskapsportal.backend.apps.api.model.OppretteFarskapserklaeringRequest;
import no.nav.farskapsportal.backend.apps.api.model.OppretteFarskapserklaeringResponse;
import no.nav.farskapsportal.backend.apps.api.model.Skriftspraak;
import no.nav.farskapsportal.backend.apps.api.model.StatusSignering;
import no.nav.farskapsportal.backend.libs.dto.BarnDto;
import no.nav.farskapsportal.backend.libs.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.backend.libs.dto.ForelderDto;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.dto.Rolle;
import no.nav.farskapsportal.backend.libs.entity.*;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.farskapsportal.backend.libs.felles.exception.EsigneringStatusFeiletException;
import no.nav.farskapsportal.backend.libs.felles.exception.FeilNavnOppgittException;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
import no.nav.farskapsportal.backend.libs.felles.exception.KontrollereNavnFarException;
import no.nav.farskapsportal.backend.libs.felles.exception.MappingException;
import no.nav.farskapsportal.backend.libs.felles.exception.PersonIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.exception.ValideringException;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Slf4j
public class FarskapsportalService {

  public static final String FEIL_NAVN =
      "Oppgitt navn til person stemmer ikke med navn slik det er registreret i Folkeregisteret";
  public static String KODE_LAND_NORGE = "NOR";
  private final FarskapsportalApiEgenskaper farskapsportalApiEgenskaper;
  private final PdfGeneratorConsumer pdfGeneratorConsumer;
  private final DifiESignaturConsumer difiESignaturConsumer;
  private final PersistenceService persistenceService;
  private final PersonopplysningService personopplysningService;
  private final BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private final BucketConsumer bucketConsumer;
  private final Mapper mapper;

  public BrukerinformasjonResponse henteBrukerinformasjon(String fnrPaaloggetBruker) {

    SIKKER_LOGG.info("Henter brukerinformasjon for person med ident {}", fnrPaaloggetBruker);

    var brukersForelderrolle = personopplysningService.bestemmeForelderrolle(fnrPaaloggetBruker);
    Set<FarskapserklaeringDto> avventerSignereringPaaloggetBruker = new HashSet<>();
    Set<FarskapserklaeringDto> avventerSigneringMotpart = new HashSet<>();
    Set<FarskapserklaeringDto> avventerRegistreringSkatt = new HashSet<>();
    Set<String> nyligFoedteBarnSomManglerFar = new HashSet<>();
    var kanOppretteFarskapserklaering = false;

    // Avbryte videre flyt dersom bruker ikke er myndig eller har en rolle som ikke støttes av
    // løsningen
    validereTilgangBasertPaaAlderOgForeldrerolle(fnrPaaloggetBruker, brukersForelderrolle);

    if (Forelderrolle.MOR.equals(brukersForelderrolle)
        || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) {

      nyligFoedteBarnSomManglerFar = validereMor(fnrPaaloggetBruker);
      kanOppretteFarskapserklaering = true;

      var morsAktiveErklaeringer =
          persistenceService.henteMorsEksisterendeErklaeringer(fnrPaaloggetBruker);

      var morsAktiveErklaeringerDto =
          morsAktiveErklaeringer.stream()
              .filter(Objects::nonNull)
              .map(mapper::toDto)
              .collect(Collectors.toSet());

      // Erklæringer som mangler mors signatur
      avventerSignereringPaaloggetBruker =
          morsAktiveErklaeringerDto.stream()
              .filter(Objects::nonNull)
              .filter(fe -> fe.getDokument().getSignertAvMor() == null)
              .collect(Collectors.toSet());
      avventerSignereringPaaloggetBruker.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.MOR));

      // Hente mors erklæringer som bare mangler fars signatur
      avventerSigneringMotpart =
          morsAktiveErklaeringerDto.stream()
              .filter(Objects::nonNull)
              .filter(fe -> fe.getDokument().getSignertAvMor() != null)
              .filter(fe -> fe.getDokument().getSignertAvFar() == null)
              .collect(Collectors.toSet());
      avventerSigneringMotpart.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.MOR));

      // Mors erklaeringer som er signert av begge foreldrene
      avventerRegistreringSkatt =
          morsAktiveErklaeringerDto.stream()
              .filter(Objects::nonNull)
              .filter(fe -> fe.getDokument().getSignertAvMor() != null)
              .filter(fe -> fe.getDokument().getSignertAvFar() != null)
              .collect(Collectors.toSet());
      avventerRegistreringSkatt.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.MOR));
    }

    if (Forelderrolle.FAR.equals(brukersForelderrolle)
        || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) {

      var farsAktiveErklaeringer = persistenceService.henteFarsErklaeringer(fnrPaaloggetBruker);

      var farsAktiveErklaeringerDto =
          farsAktiveErklaeringer.stream()
              .filter(Objects::nonNull)
              .map(mapper::toDto)
              .collect(Collectors.toSet());

      // Mangler fars signatur
      avventerSignereringPaaloggetBruker.addAll(
          farsAktiveErklaeringerDto.stream()
              .filter(Objects::nonNull)
              .filter(fe -> null == fe.getDokument().getSignertAvFar())
              .collect(Collectors.toSet()));
      avventerSignereringPaaloggetBruker.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.FAR));

      // Avventer registrering hos Skatt. For rolle MOR_ELLER_FAR kan lista allerede inneholde
      // innslag for mor
      avventerRegistreringSkatt.addAll(
          farsAktiveErklaeringerDto.stream()
              .filter(Objects::nonNull)
              .filter(fe -> null != fe.getDokument().getSignertAvFar())
              .collect(Collectors.toSet()));
      avventerRegistreringSkatt.forEach(fe -> fe.setPaaloggetBrukersRolle(Rolle.FAR));
    }

    var brukersNavnDto = personopplysningService.henteNavn(fnrPaaloggetBruker);

    return BrukerinformasjonResponse.builder()
        .brukersFornavn(brukersNavnDto.getFornavn())
        .forelderrolle(brukersForelderrolle)
        .avventerSigneringMotpart(avventerSigneringMotpart)
        .fnrNyligFoedteBarnUtenRegistrertFar(nyligFoedteBarnSomManglerFar)
        .gyldigForelderrolle(true)
        .kanOppretteFarskapserklaering(kanOppretteFarskapserklaering)
        .avventerSigneringBruker(avventerSignereringPaaloggetBruker)
        .avventerRegistrering(avventerRegistreringSkatt)
        .build();
  }

  @Transactional
  public OppretteFarskapserklaeringResponse oppretteFarskapserklaering(
      String fnrMor, OppretteFarskapserklaeringRequest request) {

    // Sjekker om mor skal kunne opprette ny farskapserklæring
    validereTilgangMor(fnrMor, request);

    var barnDto = oppretteBarnDto(request);
    var forelderDtoMor = oppretteForelderDto(fnrMor);
    var forelderDtoFar = oppretteForelderDto(request.getOpplysningerOmFar().getFoedselsnummer());

    var dokument =
        Dokument.builder()
            .navn("Farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder().sendtTilSignering(LocalDateTime.now()).build())
            .build();

    log.info("Lagre farskapserklæring");
    var farskapserklaering =
        persistenceService.lagreNyFarskapserklaering(
            Farskapserklaering.builder()
                .barn(mapper.toEntity(barnDto))
                .mor(mapper.toEntity(forelderDtoMor))
                .far(mapper.toEntity(forelderDtoFar))
                .dokument(dokument)
                .build());

    var innhold =
        pdfGeneratorConsumer.genererePdf(
            barnDto, forelderDtoMor, forelderDtoFar, request.getSkriftspraak());

    var blobIdGcp = bucketConsumer.lagrePades(farskapserklaering.getId(), innhold);

    farskapserklaering.getDokument().setBlobIdGcp(blobIdGcp);

    // Opprette signeringsjobb, oppdaterer dokument med status-url og redirect-urler
    difiESignaturConsumer.oppretteSigneringsjobb(
        farskapserklaering.getId(),
        farskapserklaering.getDokument(),
        innhold,
        request.getSkriftspraak() == null ? Skriftspraak.BOKMAAL : request.getSkriftspraak(),
        mapper.toEntity(forelderDtoMor),
        mapper.toEntity(forelderDtoFar));

    return OppretteFarskapserklaeringResponse.builder()
        .redirectUrlForSigneringMor(dokument.getSigneringsinformasjonMor().getRedirectUrl())
        .build();
  }

  public void kontrollereFar(String fnrMor, KontrollerePersonopplysningerRequest request) {
    validereAtMorOgFarIkkeDelerFoedselsnummer(fnrMor, request.getFoedselsnummer());
    antallsbegrensetKontrollAvNavnOgNummerPaaFar(fnrMor, request);
    try {
      validereFar(request.getFoedselsnummer());
    } catch (ValideringException valideringException) {
      log.warn("Kontroll av far feilet med kode {}", valideringException.getFeilkode());
      // Maskerer feil i respons
      throw new ValideringException(Feilkode.UGYLDIG_FAR);
    }
  }

  public Set<String> validereMor(String fnrMor) {

    // Mor kan ikke være død
    validereAtForelderIkkeErDoed(fnrMor);

    // Mor må være myndig (dvs er over 18 år og ingen verge)
    validereAtForelderErMyndig(fnrMor);

    // Mor kan ikke være registrert med dnummer
    validereAtPersonHarAktivtFoedselsnummer(fnrMor, Rolle.MOR);

    // Mor må ha norsk bostedsadresse
    validereMorErBosattINorge(fnrMor);

    // Mors ektefelle registreres automatisk som far etter norsk lov dersom mor er gift - gifte
    // mødre får derfor ikke opprette farskapserklæring
    validereSivilstand(fnrMor);

    // har mor noen nyfødte barn uten registrert far?
    Set<String> nyligFoedteBarnSomManglerFar =
        personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

    // Sjekke at mor tilfredsstiller rollekrav for mor med mindre hun har nyfødt barn uten
    // registrert far
    if (nyligFoedteBarnSomManglerFar.size() < 1) {
      log.info("Mor er ikke registrert med nyfødte barn uten far");
      SIKKER_LOGG.info("Mor ({}) er ikke registrert med nyfødte barn uten far", fnrMor);
      riktigRolleForOpprettingAvErklaering(fnrMor);
    } else if (nyligFoedteBarnSomManglerFar.size() > 1) {
      log.info(
          "Mor har mer enn én nyfødt uten registrert far. Antall nyfødte er {}",
          nyligFoedteBarnSomManglerFar.size());
      SIKKER_LOGG.info(
          "Mor ({}) har mer enn én nyfødt uten registrert far. Antall nyfødte er {}",
          fnrMor,
          nyligFoedteBarnSomManglerFar.size());
    }
    return nyligFoedteBarnSomManglerFar;
  }

  /**
   * Oppdaterer status på signeringsjobb. Kalles etter at bruker har fullført signering. Lagrer
   * pades-url for fremtidige dokument-nedlastinger (Transactional)
   *
   * @param fnrPaaloggetPerson fødselsnummer til pålogget person
   * @param statusQueryToken tilgangstoken fra e-signeringsløsningen
   * @return kopi av signert dokument
   */
  @Transactional(noRollbackFor = EsigneringStatusFeiletException.class)
  public FarskapserklaeringDto oppdatereStatusSigneringsjobb(
      String fnrPaaloggetPerson, int idFarskapserklaering, String statusQueryToken) {

    log.info("Oppdaterer status på signeringsoppdrag for pålogget person");

    if (idFarskapserklaering > 0) {
      return oppdatereStatusSigneringsjobb(
          idFarskapserklaering, statusQueryToken, fnrPaaloggetPerson);
    }

    var forelderrolle = personopplysningService.bestemmeForelderrolle(fnrPaaloggetPerson);
    var forelder = persistenceService.henteForelderForIdent(fnrPaaloggetPerson);

    log.error(
        "Fant ikke farskapserklæring med id {} i databasen for person med forelderrolle {} og forelderid {}.",
        idFarskapserklaering,
        forelderrolle,
        forelder.isPresent() ? forelder.get().getId() : "ukjent");

    throw new RessursIkkeFunnetException(Feilkode.FANT_IKKE_FARSKAPSERKLAERING);
  }

  @Transactional(noRollbackFor = EsigneringStatusFeiletException.class)
  public void synkronisereSigneringsstatusFar(int idFarskapserklaering) {
    log.info(
        "Oppdaterer status på fars signeringsjobb i farskapserklaering med id {}",
        idFarskapserklaering);
    var farskapserklaering = persistenceService.henteFarskapserklaeringForId(idFarskapserklaering);

    var statusQueryToken = farskapserklaering.getDokument().getStatusQueryToken();

    if (statusQueryToken != null && statusQueryToken.length() > 0) {
      var respons =
          oppdatereStatusSigneringsjobb(
              idFarskapserklaering,
              farskapserklaering.getDokument().getStatusQueryToken(),
              farskapserklaering.getFar().getFoedselsnummer());
      log.info(
          "Signeringstatus synkronisert for farskapserklæring med id {}, far signerte {}",
          idFarskapserklaering,
          respons.getDokument().getSignertAvFar());
    } else {
      log.info(
          "Status-query-token manglet for farskapserklæring med id {}. Fikk derfor ikke oppdatert signeringsstatus.",
          idFarskapserklaering);
    }
  }

  private FarskapserklaeringDto oppdatereStatusSigneringsjobb(
      int idFarskapserklaering, String statusQueryToken, String fnrPaaloggetPerson) {
    var farskapserklaering = persistenceService.henteFarskapserklaeringForId(idFarskapserklaering);

    if (farskapserklaering == null) {
      throw new ValideringException(Feilkode.PERSON_HAR_INGEN_VENTENDE_FARSKAPSERKLAERINGER);
    }

    // Henter status på signeringsjobben fra Postens signeringstjeneste
    var dokumentStatusDto = henteDokumentstatus(statusQueryToken, farskapserklaering);

    farskapserklaering.getDokument().setStatusQueryToken(statusQueryToken);

    validereAtForeldreIkkeAlleredeHarSignert(fnrPaaloggetPerson, farskapserklaering);

    log.info("Oppdaterer signeringsinfo for pålogget person");
    oppdatereSigneringsinfoForPaaloggetPerson(
        fnrPaaloggetPerson, dokumentStatusDto, farskapserklaering);

    return mapper.toDto(farskapserklaering);
  }

  @Transactional
  public URI henteNyRedirectUrl(String fnrPaaloggetPerson, int idFarskapserklaering) {
    var farskapserklaering = persistenceService.henteFarskapserklaeringForId(idFarskapserklaering);
    validereAtPersonErForelderIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering);
    validereAtPaaloggetPersonIkkeAlleredeHarSignert(fnrPaaloggetPerson, farskapserklaering);

    var undertegnerUrl = velgeRiktigUndertegnerUrl(fnrPaaloggetPerson, farskapserklaering);
    var nyRedirectUrl = difiESignaturConsumer.henteNyRedirectUrl(undertegnerUrl);

    if (personErFarIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering)) {
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setRedirectUrl(nyRedirectUrl.toString());
    } else {
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setRedirectUrl(nyRedirectUrl.toString());
    }

    return nyRedirectUrl;
  }

  @Transactional
  public OppdatereFarskapserklaeringResponse oppdatereFarskapserklaeringMedFarBorSammenInfo(
      String fnrPaaloggetPerson, OppdatereFarskapserklaeringRequest request) {

    var farskapserklaering =
        persistenceService.henteFarskapserklaeringForId(request.getIdFarskapserklaering());
    validereAtPersonErForelderIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering);

    if (personErFarIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering)) {
      farskapserklaering.setFarBorSammenMedMor(request.getFarBorSammenMedMor().booleanValue());
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSendtTilSignering(LocalDateTime.now());
    } else {
      throw new ValideringException(Feilkode.BOR_SAMMEN_INFO_KAN_BARE_OPPDATERES_AV_FAR);
    }

    return OppdatereFarskapserklaeringResponse.builder()
        .oppdatertFarskapserklaeringDto(mapper.toDto(farskapserklaering))
        .build();
  }

  @Transactional
  public byte[] henteDokumentinnhold(String fnrForelder, int idFarskapserklaering) {
    var farskapserklaering = persistenceService.henteFarskapserklaeringForId(idFarskapserklaering);

    validereAtPersonErForelderIFarskapserklaering(fnrForelder, farskapserklaering);

    if (farskapserklaering.getSendtTilSkatt() != null) {
      var blobIdGcp = farskapserklaering.getDokument().getBlobIdGcp();
      if (blobIdGcp == null) {

        // TODO: Fjerne henting av dokument fra dokumentinnhold-tabell etter buckets-migrering er
        // fullført
        if (henteDokumentFraDatabase(farskapserklaering).isPresent()) {
          return henteDokumentFraDatabase(farskapserklaering).get();
        }

        log.error(
            "Feil ved henting av PAdES til nedlasting for farskapserklaering med id {}",
            farskapserklaering.getId());
        return null;
      }
      return bucketConsumer.getContentFromBucket(blobIdGcp);
    } else {
      return beggeForeldreHarSignert(farskapserklaering)
          ? hentePadesFraPosten(farskapserklaering)
          : null;
    }
  }

  public byte[] hentePadesFraPosten(Farskapserklaering farskapserklaering) {
    var signeringsstatus = henteDokumentstatus(farskapserklaering);

    log.info(
        "Henter oppdaterte signeringsdokumenter fra esigneringstjenesten for farskapserklaering med id {}",
        farskapserklaering.getId());

    return difiESignaturConsumer.henteSignertDokument(signeringsstatus.getPadeslenke());
  }

  @Transactional
  public byte[] henteOgLagrePades(Farskapserklaering farskapserklaering) {

    // Ikke oppdatere dersom farskapserklæringen er sendt til Skatt
    if (farskapserklaering.getSendtTilSkatt() != null) {
      return null;
    }

    var pades = hentePadesFraPosten(farskapserklaering);

    if (pades == null) {
      log.error(
          "Henting av signering dokument feilet for farskapserklæring med id {}.",
          farskapserklaering.getId());
      throw new InternFeilException(Feilkode.DOKUMENT_MANGLER_INNOHLD);
    }

    var blobIdGcp = bucketConsumer.lagrePades(farskapserklaering.getId(), pades);

    farskapserklaering.getDokument().setBlobIdGcp(blobIdGcp);

    // TODO: Fjerne når bucket-migrering er fullført
    farskapserklaering
        .getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold(null).build());

    if (farskapserklaering.getMeldingsidSkatt() == null) {
      farskapserklaering.setMeldingsidSkatt(getMeldingsidSkatt(farskapserklaering, pades));
    }

    return pades;
  }

  public void henteOgLagreXadesXml(Farskapserklaering farskapserklaering) {

    var signeringsstatus = henteDokumentstatus(farskapserklaering);

    for (SignaturDto signatur : signeringsstatus.getSignaturer()) {
      var xades = difiESignaturConsumer.henteXadesXml(signatur.getXadeslenke());
      if (signatur.getSignatureier().equals(farskapserklaering.getMor().getFoedselsnummer())
          && xades != null) {

        var eksisterendeBlobIdGcp =
            bucketConsumer.getExistingBlobIdGcp(
                bucketConsumer.getBucketName(BucketConsumer.ContentType.XADES),
                "xades-mor-" + farskapserklaering.getId() + ".xml");

        var blobIdGcp =
            eksisterendeBlobIdGcp.isPresent()
                ? eksisterendeBlobIdGcp.get()
                : bucketConsumer.lagreXadesMor(farskapserklaering.getId(), xades);

        farskapserklaering.getDokument().getSigneringsinformasjonMor().setBlobIdGcp(blobIdGcp);

        // TODO: Fjerne når bucket-migrering er fullført
        farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml(null);

      } else if (signatur.getSignatureier().equals(farskapserklaering.getFar().getFoedselsnummer())
          && xades != null) {

        var eksisterendeBlobIdGcp =
            bucketConsumer.getExistingBlobIdGcp(
                bucketConsumer.getBucketName(BucketConsumer.ContentType.XADES),
                "xades-far-" + farskapserklaering.getId() + ".xml");
        var blobIdGcp =
            eksisterendeBlobIdGcp.isPresent()
                ? eksisterendeBlobIdGcp.get()
                : bucketConsumer.lagreXadesFar(farskapserklaering.getId(), xades);

        farskapserklaering.getDokument().getSigneringsinformasjonFar().setBlobIdGcp(blobIdGcp);

        // TODO: Fjerne når bucket-migrering er fullført
        farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml(null);

      } else {
        log.error(
            "Personer i signeringsoppdrag stemmer ikke med foreldrene i farskapserklæring med id {}",
            farskapserklaering.getId());
        SIKKER_LOGG.error(
            "Person i signeringsoppdrag (personident: {}), er forskjellig fra foreldrene i farskapserklæring med id {}",
            signatur.getSignatureier(),
            farskapserklaering.getId());
        throw new InternFeilException(Feilkode.FARSKAPSERKLAERING_HAR_INKONSISTENTE_DATA);
      }
    }
  }

  // TODO: Fjerne etter at GCP bucket-migrering er fullført
  private Optional<byte[]> henteDokumentFraDatabase(Farskapserklaering farskapserklaering) {
    return farskapserklaering.getDokument().getDokumentinnhold() != null
        ? Optional.of(farskapserklaering.getDokument().getDokumentinnhold().getInnhold())
        : Optional.empty();
  }

  private void validereAtForeldreIkkeAlleredeHarSignert(
      String fnrPaaloggetPerson, Farskapserklaering aktuellFarskapserklaering) {
    if (fnrPaaloggetPerson.equals(aktuellFarskapserklaering.getMor().getFoedselsnummer())
        && aktuellFarskapserklaering
                .getDokument()
                .getSigneringsinformasjonMor()
                .getSigneringstidspunkt()
            != null) {
      throw new ValideringException(Feilkode.MOR_HAR_ALLEREDE_SIGNERT);
    } else if (fnrPaaloggetPerson.equals(aktuellFarskapserklaering.getFar().getFoedselsnummer())
        && aktuellFarskapserklaering
                .getDokument()
                .getSigneringsinformasjonFar()
                .getSigneringstidspunkt()
            != null) {
      throw new ValideringException(Feilkode.FAR_HAR_ALLEREDE_SIGNERT);
    }
  }

  private void oppdatereSigneringsinfoForPaaloggetPerson(
      String fnrPaaloggetPerson,
      DokumentStatusDto dokumentStatusDto,
      Farskapserklaering aktuellFarskapserklaering) {
    oppdatereSigneringsinfo(
        Optional.of(fnrPaaloggetPerson), dokumentStatusDto, aktuellFarskapserklaering);
  }

  private Farskapserklaering oppdatereSigneringsinfo(
      Optional<String> fnrPaaloggetPerson,
      DokumentStatusDto dokumentStatusDto,
      Farskapserklaering aktuellFarskapserklaering) {

    // Oppdatere foreldrenes signeringsinfo
    for (SignaturDto signatur : dokumentStatusDto.getSignaturer()) {

      var skalOppdatereForMor =
          fnrPaaloggetPerson.isEmpty()
              || fnrPaaloggetPerson
                  .get()
                  .equals(aktuellFarskapserklaering.getMor().getFoedselsnummer());
      var skalOppdatereForFar =
          fnrPaaloggetPerson.isEmpty()
              || fnrPaaloggetPerson
                  .get()
                  .equals(aktuellFarskapserklaering.getFar().getFoedselsnummer());

      // Oppdatere for mor
      if (skalOppdatereForMor
          && aktuellFarskapserklaering
              .getMor()
              .getFoedselsnummer()
              .equals(signatur.getSignatureier())) {
        return oppdatereSigneringsinfoForMor(
            dokumentStatusDto, aktuellFarskapserklaering, signatur);

        // Oppdatere for far
      } else if (skalOppdatereForFar
          && aktuellFarskapserklaering
              .getFar()
              .getFoedselsnummer()
              .equals(signatur.getSignatureier())) {
        return oppdatereSigneringsinfoForFar(
            dokumentStatusDto, aktuellFarskapserklaering, signatur);
      }
    }

    throw new RessursIkkeFunnetException(Feilkode.OPPDATERING_IKKE_MULIG);
  }

  @Nullable
  private Farskapserklaering oppdatereSigneringsinfoForFar(
      DokumentStatusDto dokumentStatusDto,
      Farskapserklaering aktuellFarskapserklaering,
      SignaturDto signatur) {

    haandetereStatusFeilet(dokumentStatusDto, aktuellFarskapserklaering, Rolle.FAR);

    if (!dokumentStatusDto.getStatusSignering().equals(StatusSignering.SUKSESS)) {
      aktuellFarskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setStatusSignering(dokumentStatusDto.getStatusSignering().toString());
      return aktuellFarskapserklaering;
    }

    aktuellFarskapserklaering
        .getDokument()
        .setBekreftelsesUrl(dokumentStatusDto.getBekreftelseslenke().toString());
    aktuellFarskapserklaering
        .getDokument()
        .setPadesUrl(dokumentStatusDto.getPadeslenke().toString());

    if (aktuellFarskapserklaering.getSendtTilSkatt() == null && signatur.isHarSignert()) {
      aktuellFarskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(signatur.getTidspunktForStatus().toLocalDateTime());
      if (farskapsportalApiEgenskaper.isBrukernotifikasjonerPaa()) {
        // Slette fars oppgave for signering på DittNav
        var aktiveOppgaver =
            persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(
                aktuellFarskapserklaering.getId(), aktuellFarskapserklaering.getFar());
        for (Oppgavebestilling oppgave : aktiveOppgaver) {
          brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(
              oppgave.getEventId(), aktuellFarskapserklaering.getFar());
        }
        // Informere foreldrene om gjennomført signering og tilgjengelig farskapserklæring
        brukernotifikasjonConsumer.informereForeldreOmTilgjengeligFarskapserklaering(
            aktuellFarskapserklaering.getMor(), aktuellFarskapserklaering.getFar());
      }

      return aktuellFarskapserklaering;
    }
    return null;
  }

  @Nullable
  private Farskapserklaering oppdatereSigneringsinfoForMor(
      DokumentStatusDto dokumentStatusDto,
      Farskapserklaering aktuellFarskapserklaering,
      SignaturDto signatur) {

    haandetereStatusFeilet(dokumentStatusDto, aktuellFarskapserklaering, Rolle.MOR);

    if (!dokumentStatusDto.getStatusSignering().equals(StatusSignering.PAAGAAR)) {
      aktuellFarskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setStatusSignering(dokumentStatusDto.getStatusSignering().toString());
      return aktuellFarskapserklaering;
    }

    aktuellFarskapserklaering
        .getDokument()
        .setPadesUrl(dokumentStatusDto.getPadeslenke().toString());
    aktuellFarskapserklaering
        .getDokument()
        .setBekreftelsesUrl(dokumentStatusDto.getBekreftelseslenke().toString());

    if (signatur.isHarSignert()
        && aktuellFarskapserklaering
                .getDokument()
                .getSigneringsinformasjonMor()
                .getSigneringstidspunkt()
            == null) {
      aktuellFarskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(signatur.getTidspunktForStatus().toLocalDateTime());
      if (farskapsportalApiEgenskaper.isBrukernotifikasjonerPaa()) {
        brukernotifikasjonConsumer.oppretteOppgaveTilFarOmSignering(
            aktuellFarskapserklaering.getId(), aktuellFarskapserklaering.getFar());
      }
      return aktuellFarskapserklaering;
    }
    return null;
  }

  private void haandetereStatusFeilet(
      DokumentStatusDto dokumentStatusDto, Farskapserklaering farskapserklaering, Rolle rolle) {
    if (dokumentStatusDto.getStatusSignering().equals(StatusSignering.FEILET)) {
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setStatusSignering(dokumentStatusDto.getStatusSignering().toString());
      farskapserklaering.setDeaktivert(LocalDateTime.now());

      if (rolle.equals(Rolle.FAR)) {
        farskapserklaering
            .getDokument()
            .getSigneringsinformasjonFar()
            .setStatusSignering(dokumentStatusDto.getStatusSignering().toString());
        if (farskapsportalApiEgenskaper.isBrukernotifikasjonerPaa()) {
          brukernotifikasjonConsumer.varsleOmAvbruttSignering(
              farskapserklaering.getMor(), farskapserklaering.getFar());
          var farsAktiveSigneringsoppgaver =
              persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(
                  farskapserklaering.getId(), farskapserklaering.getFar());
          log.info(
              "Fant {} aktiv signeringsoppgave til knyttet til far med id {}",
              farsAktiveSigneringsoppgaver.size(),
              farskapserklaering.getFar().getId());
          for (Oppgavebestilling oppgave : farsAktiveSigneringsoppgaver) {
            brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(
                oppgave.getEventId(), farskapserklaering.getFar());
          }
        }
      }

      throw new EsigneringStatusFeiletException(
          Feilkode.ESIGNERING_STATUS_FEILET, farskapserklaering);
    }
  }

  private void validereTilgangBasertPaaAlderOgForeldrerolle(
      String foedselsnummer, Forelderrolle forelderrolle) {

    // Kun myndige personer kan bruke løsningen
    validereAtForelderErMyndig(foedselsnummer);

    // Løsningen er ikke åpen for medmor eller person med udefinerbar forelderrolle
    if (Forelderrolle.MEDMOR.equals(forelderrolle) || Forelderrolle.UKJENT.equals(forelderrolle)) {
      throw new ValideringException(Feilkode.MEDMOR_ELLER_UKJENT);
    }
  }

  private void validereSivilstand(String foedselsnummer) {
    var sivilstand = personopplysningService.henteSivilstand(foedselsnummer);
    switch (sivilstand.getType()) {
      case GIFT -> throw new ValideringException(Feilkode.MOR_SIVILSTAND_GIFT);
      case REGISTRERT_PARTNER -> throw new ValideringException(
          Feilkode.MOR_SIVILSTAND_REGISTRERT_PARTNER);
      case UOPPGITT -> throw new ValideringException(Feilkode.MOR_SIVILSTAND_UOPPGITT);
    }
  }

  private void riktigRolleForOpprettingAvErklaering(String fnrPaaloggetPerson) {
    log.info("Sjekker om person kan opprette farskapserklaering..");
    SIKKER_LOGG.info(
        "Sjekker om person ({}) kan opprette farskapserklaering..", fnrPaaloggetPerson);

    var forelderrolle = personopplysningService.bestemmeForelderrolle(fnrPaaloggetPerson);
    var paaloggetPersonKanOpptreSomMor =
        Forelderrolle.MOR.equals(forelderrolle)
            || Forelderrolle.MOR_ELLER_FAR.equals(forelderrolle);

    if (!paaloggetPersonKanOpptreSomMor) {
      throw new ValideringException(Feilkode.FEIL_ROLLE_OPPRETTE);
    }
  }

  private ForelderDto oppretteForelderDto(String foedseslnummer) {
    var navnDto = personopplysningService.henteNavn(foedseslnummer);
    var foedselsdato = personopplysningService.henteFoedselsdato(foedseslnummer);
    return ForelderDto.builder()
        .foedselsnummer(foedseslnummer)
        .foedselsdato(foedselsdato)
        .navn(navnDto)
        .build();
  }

  private BarnDto oppretteBarnDto(OppretteFarskapserklaeringRequest request) {
    var foedselsnummer = request.getBarn().getFoedselsnummer();
    if (foedselsnummer != null && !foedselsnummer.isBlank()) {
      var foedselsdato = personopplysningService.henteFoedselsdato(foedselsnummer);
      var foedested = personopplysningService.henteFoedested(request.getBarn().getFoedselsnummer());
      return BarnDto.builder()
          .foedested(foedested)
          .foedselsdato(foedselsdato)
          .foedselsnummer(foedselsnummer)
          .build();
    } else {
      return BarnDto.builder().termindato(request.getBarn().getTermindato()).build();
    }
  }

  private void antallsbegrensetKontrollAvNavnOgNummerPaaFar(
      String fnrMor, KontrollerePersonopplysningerRequest request) {
    var statusKontrollereFar = persistenceService.henteStatusKontrollereFar(fnrMor);
    var registrertNavnFar = hentRegistrertNavnPaaOppgittFar(fnrMor, request);
    if (statusKontrollereFar.isEmpty()
        || farskapsportalApiEgenskaper
                .getFarskapsportalFellesEgenskaper()
                .getKontrollFarMaksAntallForsoek()
            > statusKontrollereFar.get().getAntallFeiledeForsoek()) {
      kontrollereNavnOgNummerFar(fnrMor, request.getNavn(), registrertNavnFar.sammensattNavn());
    } else {
      throw berikeOgKasteKontrollereNavnFarException(
          fnrMor,
          new FeilNavnOppgittException(Feilkode.MAKS_ANTALL_FORSOEK, request.getNavn(), null));
    }
  }

  private void kontrollereNavnOgNummerFar(
      String fnrMor, String oppgittNavnFar, String registrertNavnFar) {
    try {
      validereOppgittNavnFar(oppgittNavnFar, registrertNavnFar);
    } catch (KontrollereNavnFarException e) {
      throw berikeOgKasteKontrollereNavnFarException(fnrMor, e);
    }
  }

  private KontrollereNavnFarException berikeOgKasteKontrollereNavnFarException(
      String fnrMor, KontrollereNavnFarException e) {
    var statusKontrollereFarDto =
        mapper.toDto(
            persistenceService.oppdatereStatusKontrollereFar(
                fnrMor,
                e.getNavnIRegister(),
                e.getOppgittNavn(),
                farskapsportalApiEgenskaper.getKontrollFarForsoekFornyesEtterAntallDager(),
                farskapsportalApiEgenskaper
                    .getFarskapsportalFellesEgenskaper()
                    .getKontrollFarMaksAntallForsoek()));
    e.setStatusKontrollereFarDto(Optional.of(statusKontrollereFarDto));
    var resterendeAntallForsoek =
        farskapsportalApiEgenskaper
                .getFarskapsportalFellesEgenskaper()
                .getKontrollFarMaksAntallForsoek()
            - statusKontrollereFarDto.getAntallFeiledeForsoek();
    resterendeAntallForsoek = Math.max(resterendeAntallForsoek, 0);
    statusKontrollereFarDto.setAntallResterendeForsoek(resterendeAntallForsoek);
    return e;
  }

  private NavnDto hentRegistrertNavnPaaOppgittFar(
      String fnrMor, KontrollerePersonopplysningerRequest request) {
    if (request.getFoedselsnummer() == null || request.getFoedselsnummer().trim().length() < 1) {
      throw new ValideringException(Feilkode.FOEDSELNUMMER_MANGLER_FAR);
    }
    try {
      return personopplysningService.henteNavn(request.getFoedselsnummer());
    } catch (RessursIkkeFunnetException rife) {
      throw berikeOgKasteKontrollereNavnFarException(
          fnrMor, new PersonIkkeFunnetException(request.getNavn(), null));
    }
  }

  private void validereOppgittNavnFar(String oppgittNavnPaaFar, String navnFraFolkeregisteret) {

    if (oppgittNavnPaaFar == null || oppgittNavnPaaFar.trim().length() < 1) {
      throw new ValideringException(Feilkode.KONTROLLERE_FAR_NAVN_MANGLER);
    }

    personopplysningService.navnekontroll(oppgittNavnPaaFar, navnFraFolkeregisteret);
  }

  private void validereFar(String foedselsnummer) {

    // Far kan ikke være død
    validereAtForelderIkkeErDoed(foedselsnummer);

    // Far må være myndig (dvs er over 18 år og ingen verge)
    validereAtForelderErMyndig(foedselsnummer);

    var farsForelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

    // Far må ha foreldrerolle FAR eller MOR_ELLER_FAR
    if (!(Forelderrolle.FAR.equals(farsForelderrolle)
        || Forelderrolle.MOR_ELLER_FAR.equals(farsForelderrolle))) {
      throw new ValideringException(Feilkode.FEIL_ROLLE_FAR);
    }

    // Far skal ikke være registrert med dnummer.
    validereAtPersonHarAktivtFoedselsnummer(foedselsnummer, Rolle.FAR);
  }

  private void validereAtPersonHarAktivtFoedselsnummer(String foedselsnummer, Rolle rolle) {
    var folkeregisteridentifikatorDto =
        personopplysningService.henteFolkeregisteridentifikator(foedselsnummer);
    try {
      Validate.isTrue(folkeregisteridentifikatorDto.getType().equalsIgnoreCase("FNR"));
      Validate.isTrue(folkeregisteridentifikatorDto.getStatus().equalsIgnoreCase("I_BRUK"));
    } catch (IllegalArgumentException iae) {
      throw new ValideringException(henteFeilkodeForManglerFnummer(rolle));
    }
  }

  private Feilkode henteFeilkodeForManglerFnummer(Rolle rolle) {
    switch (rolle) {
      case MOR:
        return Feilkode.MOR_HAR_IKKE_FNUMMER;
      case BARN:
        return Feilkode.BARN_HAR_IKKE_FNUMMER;
      default:
        return Feilkode.FAR_HAR_IKKE_FNUMMER;
    }
  }

  private void validereAtForelderErMyndig(String foedselsnummer) {
    if (!personopplysningService.erOver18Aar(foedselsnummer)) {
      throw new ValideringException(Feilkode.IKKE_MYNDIG);
    }
    if (personopplysningService.harVerge(foedselsnummer)) {
      throw new ValideringException(Feilkode.FORELDER_HAR_VERGE);
    }
  }

  private void validereAtForelderIkkeErDoed(String foedselsnummer) {
    if (personopplysningService.erDoed(foedselsnummer)) {
      throw new ValideringException(Feilkode.PERSON_ER_DOED);
    }
  }

  private void validereMorErBosattINorge(String foedselsnummer) {
    try {
      Validate.isTrue(personopplysningService.harNorskBostedsadresse(foedselsnummer));
    } catch (IllegalArgumentException iae) {
      throw new ValideringException(Feilkode.MOR_IKKE_NORSK_BOSTEDSADRESSE);
    }
  }

  private void validereTilgangMor(String fnrMor, OppretteFarskapserklaeringRequest request) {

    // Validere alder og rolle
    validereMor(fnrMor);
    // Validere alder, fødested, og relasjon til mor for eventuell nyfødt
    validereNyfoedt(fnrMor, request.getBarn().getFoedselsnummer());
    // Kontrollere at mor og far ikke er samme person
    validereAtMorOgFarIkkeDelerFoedselsnummer(
        fnrMor, request.getOpplysningerOmFar().getFoedselsnummer());
    // Validere at termindato er innenfor gyldig intervall dersom barn ikke er født
    termindatoErGyldig(request.getBarn());
    // Sjekke at ny farskapserklæring ikke kommmer i konflikt med eksisterende
    persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(
        fnrMor,
        request.getOpplysningerOmFar().getFoedselsnummer(),
        BarnDto.builder()
            .termindato(request.getBarn().getTermindato())
            .foedselsnummer(request.getBarn().getFoedselsnummer())
            .build());
  }

  private void validereNyfoedt(String fnrMor, String fnrNyfoedt) {
    if (fnrNyfoedt == null || fnrNyfoedt.length() < 1) {
      return;
    }
    validereFoedelandNorge(fnrNyfoedt);
    validereAtPersonHarAktivtFoedselsnummer(fnrNyfoedt, Rolle.BARN);
    validereAlderNyfoedt(fnrNyfoedt);
    validereRelasjonerNyfoedt(fnrMor, fnrNyfoedt);
  }

  private void validereFoedelandNorge(String fnrNyfoedt) {
    try {
      var foedeland = personopplysningService.henteFoedeland(fnrNyfoedt);
      Validate.isTrue(
          foedeland != null
              && personopplysningService
                  .henteFoedeland(fnrNyfoedt)
                  .equalsIgnoreCase(KODE_LAND_NORGE));
    } catch (IllegalArgumentException iae) {
      log.warn("Barn er født utenfor Norge!");
      throw new ValideringException(Feilkode.BARN_FOEDT_UTENFOR_NORGE);
    }
  }

  private void validereAlderNyfoedt(String fnrOppgittBarn) {
    var foedselsdato = personopplysningService.henteFoedselsdato(fnrOppgittBarn);
    if (!LocalDate.now()
        .minusMonths(
            farskapsportalApiEgenskaper
                .getFarskapsportalFellesEgenskaper()
                .getMaksAntallMaanederEtterFoedsel())
        .isBefore(foedselsdato)) {
      throw new ValideringException(Feilkode.NYFODT_ER_FOR_GAMMEL);
    }
  }

  private void validereRelasjonerNyfoedt(String fnrMor, String fnrOppgittBarn) {

    if (fnrOppgittBarn == null || fnrOppgittBarn.length() < 1) {
      log.info("Barnet er ikke oppgitt med fødselsnummer");
      return;
    }

    log.info("Validerer at nyfødt barn er relatert til mor, samt har ingen registrert far.");
    var registrerteNyfoedteUtenFar =
        personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

    registrerteNyfoedteUtenFar.stream()
        .findFirst()
        .orElseThrow(() -> new ValideringException(Feilkode.INGEN_NYFOEDTE_UTEN_FAR));

    registrerteNyfoedteUtenFar.stream()
        .filter(Objects::nonNull)
        .filter(fnrBarn -> fnrBarn.equals(fnrOppgittBarn))
        .collect(Collectors.toSet())
        .stream()
        .findAny()
        .orElseThrow(() -> new ValideringException(Feilkode.BARN_MANGLER_RELASJON_TIL_MOR));
  }

  private void validereAtMorOgFarIkkeDelerFoedselsnummer(String fnrMor, String fnrFar) {
    log.info("Sjekker at mor og far ikke er én og samme person");
    try {
      Validate.isTrue(!fnrMor.equals(fnrFar), "Mor og far kan ikke være samme person!");
    } catch (IllegalArgumentException iae) {
      log.warn("Mor og far er samme person!");
      throw new ValideringException(Feilkode.MOR_OG_FAR_SAMME_PERSON);
    }
  }

  private void termindatoErGyldig(BarnDto barnDto) {
    log.info("Validerer termindato");

    if (barnDto.getFoedselsnummer() != null
        && !barnDto.getFoedselsnummer().isBlank()
        && barnDto.getFoedselsnummer().length() > 10) {
      log.info("Termindato er ikke oppgitt");
      return;
    } else {
      var nedreGrense =
          LocalDate.now()
              .plusWeeks(farskapsportalApiEgenskaper.getMinAntallUkerTilTermindato() - 1);
      var oevreGrense =
          LocalDate.now()
              .plusWeeks(farskapsportalApiEgenskaper.getMaksAntallUkerTilTermindato() + 1);
      if (nedreGrense.isBefore(barnDto.getTermindato())
          && oevreGrense.isAfter(barnDto.getTermindato())) {
        log.info("Termindato validert");
        return;
      }
    }

    throw new ValideringException(Feilkode.TERMINDATO_UGYLDIG);
  }

  private DokumentStatusDto henteDokumentstatus(Farskapserklaering farskapserklaering) {
    return henteDokumentstatus(
        farskapserklaering.getDokument().getStatusQueryToken(), farskapserklaering);
  }

  private DokumentStatusDto henteDokumentstatus(
      String statusQueryToken, Farskapserklaering farskapserklaering) {

    log.info("Henter dokumentstatus fra Posten.");

    return difiESignaturConsumer.henteStatus(
        statusQueryToken,
        farskapserklaering.getDokument().getJobbref(),
        tilUri(farskapserklaering.getDokument().getStatusUrl()));
  }

  private URI tilUri(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException urise) {
      throw new MappingException("Lagret status-URL har feil format", urise);
    }
  }

  private void validereAtPaaloggetPersonIkkeAlleredeHarSignert(
      String fnrPaaloggetPerson, Farskapserklaering farskapserklaering) {
    boolean erMor = personErMorIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering);
    boolean erFar = personErFarIFarskapserklaering(fnrPaaloggetPerson, farskapserklaering);
    if (erMor
        && farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt()
            == null) {
      return;
    } else if (erFar
        && farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()
            == null) {
      return;
    }
    throw new ValideringException(Feilkode.PERSON_HAR_ALLEREDE_SIGNERT);
  }

  private boolean beggeForeldreHarSignert(Farskapserklaering farskapserklaering) {
    if (farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt()
            != null
        && farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()
            != null) {
      return true;
    }
    log.warn(
        "Farskapserklæring med id {} er ikke signert av begge foreldrene",
        farskapserklaering.getId());
    return false;
  }

  private void validereAtPersonErForelderIFarskapserklaering(
      String foedselsnummer, Farskapserklaering farskapserklaering) {
    if (foedselsnummer.equals(farskapserklaering.getMor().getFoedselsnummer())
        || foedselsnummer.equals(farskapserklaering.getFar().getFoedselsnummer())) {
      return;
    }
    throw new ValideringException(Feilkode.PERSON_IKKE_PART_I_FARSKAPSERKLAERING);
  }

  private boolean personErFarIFarskapserklaering(
      String foedselsnummer, Farskapserklaering farskapserklaering) {
    return foedselsnummer.equals(farskapserklaering.getFar().getFoedselsnummer());
  }

  private boolean personErMorIFarskapserklaering(
      String foedselsnummer, Farskapserklaering farskapserklaering) {
    return foedselsnummer.equals(farskapserklaering.getMor().getFoedselsnummer());
  }

  private URI velgeRiktigUndertegnerUrl(
      String foedselsnummerUndertegner, Farskapserklaering farskapserklaering) {
    try {
      return new URI(
          foedselsnummerUndertegner.equals(farskapserklaering.getMor().getFoedselsnummer())
              ? farskapserklaering.getDokument().getSigneringsinformasjonMor().getUndertegnerUrl()
              : farskapserklaering.getDokument().getSigneringsinformasjonFar().getUndertegnerUrl());
    } catch (URISyntaxException e) {
      throw new InternFeilException(Feilkode.FEILFORMATERT_URL_UNDERTEGNERURL);
    }
  }
}
