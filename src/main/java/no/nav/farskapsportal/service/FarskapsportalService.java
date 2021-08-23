package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.api.Rolle.FAR;
import static no.nav.farskapsportal.api.Rolle.MOR;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.CRC32;
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
import no.nav.farskapsportal.api.StatusSignering;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.EsigneringStatusFeiletException;
import no.nav.farskapsportal.exception.FeilNavnOppgittException;
import no.nav.farskapsportal.exception.InternFeilException;
import no.nav.farskapsportal.exception.MappingException;
import no.nav.farskapsportal.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.exception.ValideringException;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.util.Mapper;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Slf4j
public class FarskapsportalService {

  public static final String FEIL_NAVN = "Oppgitt navn til person stemmer ikke med navn slik det er registreret i Folkeregisteret";
  public static String KODE_LAND_NORGE = "NOR";
  private final FarskapsportalEgenskaper farskapsportalEgenskaper;
  private final PdfGeneratorConsumer pdfGeneratorConsumer;
  private final DifiESignaturConsumer difiESignaturConsumer;
  private final SkattConsumer skattConsumer;
  private final PersistenceService persistenceService;
  private final PersonopplysningService personopplysningService;
  private final BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private final Mapper mapper;

  private static String getUnikId(byte[] dokument, LocalDateTime tidspunktForSignering) {
    var crc32 = new CRC32();
    var outputstream = new ByteArrayOutputStream();
    outputstream.writeBytes(dokument);
    crc32.update(outputstream.toByteArray());

    var zonedDateTime = tidspunktForSignering.atZone(ZoneId.systemDefault());
    var epoch = tidspunktForSignering.toEpochSecond(zonedDateTime.getOffset());

    return String.valueOf(crc32.getValue()) + epoch;
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

      validereMor(fnrPaaloggetBruker);

      kanOppretteFarskapserklaering = true;

      // har mor noen nyfødte barn uten registrert far?
      nyligFoedteBarnSomManglerFar = personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrPaaloggetBruker);

      var morsAktiveErklaeringer = persistenceService.henteMorsEksisterendeErklaeringer(fnrPaaloggetBruker);

      // Oppdatere esigneringsstatus dersom forrige statusendring ikke er registrert
      if (farskapsportalEgenskaper.isInnhenteStatusVedPolling()) {
        morsAktiveErklaeringer = oppdatereSigneringsstatusHvisEndret(morsAktiveErklaeringer);
      }

      var morsAktiveErklaeringerDto = morsAktiveErklaeringer.stream().filter(Objects::nonNull).map(mapper::toDto).collect(Collectors.toSet());

      // Erklæringer som mangler mors signatur
      avventerSignereringPaaloggetBruker = morsAktiveErklaeringerDto.stream().filter(Objects::nonNull)
          .filter(fe -> fe.getDokument().getSignertAvMor() == null).collect(Collectors.toSet());
      avventerSignereringPaaloggetBruker.forEach(fe -> fe.setPaaloggetBrukersRolle(MOR));

      // Hente mors erklæringer som bare mangler fars signatur
      avventerSigneringMotpart = morsAktiveErklaeringerDto.stream().filter(Objects::nonNull).filter(fe -> fe.getDokument().getSignertAvMor() != null)
          .filter(fe -> fe.getDokument().getSignertAvFar() == null).collect(Collectors.toSet());
      avventerSigneringMotpart.forEach(fe -> fe.setPaaloggetBrukersRolle(MOR));
      // Oppdatere esigneringsstatus dersom forrige statusendring ikke er registrert

      // Mors erklaeringer som er signert av begge foreldrene
      avventerRegistreringSkatt = morsAktiveErklaeringerDto.stream().filter(Objects::nonNull)
          .filter(fe -> fe.getDokument().getSignertAvMor() != null).filter(fe -> fe.getDokument().getSignertAvFar() != null)
          .collect(Collectors.toSet());
      avventerRegistreringSkatt.forEach(fe -> fe.setPaaloggetBrukersRolle(MOR));
    }

    if (Forelderrolle.FAR.equals(brukersForelderrolle) || Forelderrolle.MOR_ELLER_FAR.equals(brukersForelderrolle)) {

      var farsAktiveErklaeringer = persistenceService.henteFarsErklaeringer(fnrPaaloggetBruker);

      // Oppdatere esigneringsstatus dersom forrige statusendring ikke er registrert
      if (farskapsportalEgenskaper.isInnhenteStatusVedPolling()) {
        farsAktiveErklaeringer = oppdatereSigneringsstatusHvisEndret(farsAktiveErklaeringer);
      }

      var farsAktiveErklaeringerDto = farsAktiveErklaeringer.stream().filter(Objects::nonNull).map(mapper::toDto).collect(Collectors.toSet());

      // Mangler fars signatur
      avventerSignereringPaaloggetBruker.addAll(
          farsAktiveErklaeringerDto.stream().filter(Objects::nonNull).filter(fe -> null == fe.getDokument().getSignertAvFar())
              .collect(Collectors.toSet()));
      avventerSignereringPaaloggetBruker.forEach(fe -> fe.setPaaloggetBrukersRolle(FAR));

      // Avventer registrering hos Skatt. For rolle MOR_ELLER_FAR kan lista allerede inneholde innslag for mor
      avventerRegistreringSkatt.addAll(
          farsAktiveErklaeringerDto.stream().filter(Objects::nonNull).filter(fe -> null != fe.getDokument().getSignertAvFar())
              .collect(Collectors.toSet()));
      avventerRegistreringSkatt.forEach(fe -> fe.setPaaloggetBrukersRolle(FAR));
    }

    var brukersNavnDto = personopplysningService.henteNavn(fnrPaaloggetBruker);

    return BrukerinformasjonResponse.builder().brukersFornavn(brukersNavnDto.getFornavn()).forelderrolle(brukersForelderrolle)
        .avventerSigneringMotpart(avventerSigneringMotpart)
        .fnrNyligFoedteBarnUtenRegistrertFar(nyligFoedteBarnSomManglerFar).gyldigForelderrolle(true)
        .kanOppretteFarskapserklaering(kanOppretteFarskapserklaering).avventerSigneringBruker(avventerSignereringPaaloggetBruker)
        .avventerRegistrering(avventerRegistreringSkatt).build();
  }

  @Transactional
  public OppretteFarskapserklaeringResponse oppretteFarskapserklaering(String fnrMor, OppretteFarskapserklaeringRequest request) {

    // Sjekker om mor skal kunne opprette ny farskapserklæring
    validereTilgangMor(fnrMor, request);

    // Sjekker om mor har oppgitt riktige opplysninger om far, samt at far tilfredsstiller krav til digital erklæering
    kontrollereNavnOgNummerFar(fnrMor, request.getOpplysningerOmFar());
    validereFar(request.getOpplysningerOmFar().getFoedselsnummer());

    var barnDto = oppretteBarnDto(request);
    var forelderDtoMor = oppretteForelderDto(fnrMor);
    var forelderDtoFar = oppretteForelderDto(request.getOpplysningerOmFar().getFoedselsnummer());

    var innhold = pdfGeneratorConsumer.genererePdf(barnDto, forelderDtoMor, forelderDtoFar);

    var dokument = Dokument.builder()
        .dokumentnavn("Farskapserklaering.pdf")
        .dokumentinnhold(Dokumentinnhold.builder().innhold(innhold).build())
        .build();

    // Opprette signeringsjobb, oppdaterer dokument med status-url og redirect-urler
    difiESignaturConsumer.oppretteSigneringsjobb(dokument, mapper.toEntity(forelderDtoMor), mapper.toEntity(forelderDtoFar));

    log.info("Lagre farskapserklæring");
    var farskapserklaering = Farskapserklaering.builder()
        .barn(mapper.toEntity(barnDto))
        .mor(mapper.toEntity(forelderDtoMor))
        .far(mapper.toEntity(forelderDtoFar))
        .dokument(dokument)
        .morBorSammenMedFar(request.isMorBorSammenMedFar())
        .build();

    persistenceService.lagreNyFarskapserklaering(farskapserklaering);

    return OppretteFarskapserklaeringResponse.builder().redirectUrlForSigneringMor(dokument.getSigneringsinformasjonMor().getRedirectUrl()).build();
  }

  public void kontrollereFar(String fnrMor, KontrollerePersonopplysningerRequest request) {
    validereAtMorOgFarErForskjelligePersoner(fnrMor, request.getFoedselsnummer());
    antallsbegrensetKontrollAvNavnOgNummerPaaFar(fnrMor, request);
    validereFar(request.getFoedselsnummer());
  }

  public void validereMor(String fnrMor) {

    // Mor kan ikke være død
    validereAtForelderIkkeErDoed(fnrMor);

    // Mor må være myndig (dvs er over 18 år og ingen verge)
    validereAtForelderErMyndig(fnrMor);

    // Mor må ha norsk bostedsadresse
    validereMorErBosattINorge(fnrMor);

    // Mor kan ikke være registrert med dnummer
    validereAtPersonHarAktivtFoedselsnummer(fnrMor, Rolle.MOR);

    // Bare mor kan oppretteFarskapserklæring
    riktigRolleForOpprettingAvErklaering(fnrMor);
  }

  /**
   * Oppdaterer status på signeringsjobb. Kalles etter at bruker har fullført signering. Lagrer pades-url for fremtidige dokument-nedlastinger
   * (Transactional)
   *
   * @param fnrPaaloggetPerson fødselsnummer til pålogget person
   * @param statusQueryToken tilgangstoken fra e-signeringsløsningen
   * @return kopi av signert dokument
   */
  @Transactional(noRollbackFor = EsigneringStatusFeiletException.class)
  public FarskapserklaeringDto oppdatereStatusSigneringsjobb(String fnrPaaloggetPerson, String statusQueryToken) {

    log.info("Oppdaterer status på signeringsoppdrag for pålogget person");

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

    log.info("Statuslenke tilhører farskapserklaering med id {}", aktuellFarskapserklaering.getId());

    validereAtForeldreIkkeAlleredeHarSignert(fnrPaaloggetPerson, aktuellFarskapserklaering);

    log.info("Oppdaterer signeringsinfo for pålogget person");
    oppdatereSigneringsinfoForPaaloggetPerson(fnrPaaloggetPerson, dokumentStatusDto, aktuellFarskapserklaering);

    return mapper.toDto(aktuellFarskapserklaering);
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

  private void validereAtForeldreIkkeAlleredeHarSignert(String fnrPaaloggetPerson, Farskapserklaering aktuellFarskapserklaering) {
    if (fnrPaaloggetPerson.equals(aktuellFarskapserklaering.getMor().getFoedselsnummer())
        && aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null) {
      throw new ValideringException(Feilkode.MOR_HAR_ALLEREDE_SIGNERT);
    } else if (fnrPaaloggetPerson.equals(aktuellFarskapserklaering.getFar().getFoedselsnummer())
        && aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() != null) {
      throw new ValideringException(Feilkode.FAR_HAR_ALLEREDE_SIGNERT);
    }
  }

  private void oppdatereSigneringsinfoForPaaloggetPerson(String fnrPaaloggetPerson, DokumentStatusDto dokumentStatusDto,
      Farskapserklaering aktuellFarskapserklaering) {
    oppdatereSigneringsinfo(Optional.of(fnrPaaloggetPerson), dokumentStatusDto, aktuellFarskapserklaering);
  }

  private Farskapserklaering oppdatereSigneringsinfo(Optional<String> fnrPaaloggetPerson, DokumentStatusDto dokumentStatusDto,
      Farskapserklaering aktuellFarskapserklaering) {

    // Oppdatere foreldrenes signeringsinfo
    for (SignaturDto signatur : dokumentStatusDto.getSignaturer()) {

      var skalOppdatereForMor =
          fnrPaaloggetPerson.isEmpty() || fnrPaaloggetPerson.get().equals(aktuellFarskapserklaering.getMor().getFoedselsnummer());
      var skalOppdatereForFar =
          fnrPaaloggetPerson.isEmpty() || fnrPaaloggetPerson.get().equals(aktuellFarskapserklaering.getFar().getFoedselsnummer());

      // Oppdatere for mor
      if (skalOppdatereForMor && aktuellFarskapserklaering.getMor().getFoedselsnummer().equals(signatur.getSignatureier())) {
        return oppdatereSigneringsinfoForMor(dokumentStatusDto, aktuellFarskapserklaering, signatur);

        // Oppdatere for far - sette meldingsidSkatt
      } else if (skalOppdatereForFar && aktuellFarskapserklaering.getFar().getFoedselsnummer().equals(signatur.getSignatureier())) {
        return oppdatereSigneringsinfoForFar(dokumentStatusDto, aktuellFarskapserklaering, signatur);
      }
    }

    throw new RessursIkkeFunnetException(Feilkode.OPPDATERING_IKKE_MULIG);
  }

  @Nullable
  private Farskapserklaering oppdatereSigneringsinfoForFar(DokumentStatusDto dokumentStatusDto, Farskapserklaering aktuellFarskapserklaering,
      SignaturDto signatur) {

    haandetereStatusFeilet(dokumentStatusDto, aktuellFarskapserklaering, FAR);

    if (!dokumentStatusDto.getStatusSignering().equals(StatusSignering.SUKSESS)) {
      aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().setStatusSignering(dokumentStatusDto.getStatusSignering().toString());
      return aktuellFarskapserklaering;
    }

    aktuellFarskapserklaering.getDokument().setBekreftelsesUrl(dokumentStatusDto.getBekreftelseslenke().toString());
    aktuellFarskapserklaering.getDokument().setPadesUrl(dokumentStatusDto.getPadeslenke().toString());

    if (aktuellFarskapserklaering.getSendtTilSkatt() == null && signatur.isHarSignert()) {
      aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(signatur.getTidspunktForStatus());
      aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesUrl(signatur.getXadeslenke().toString());
      var signertDokument = difiESignaturConsumer.henteSignertDokument(dokumentStatusDto.getPadeslenke());
      aktuellFarskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold(signertDokument).build());
      var xadesXml = difiESignaturConsumer.henteXadesXml(signatur.getXadeslenke());
      aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml(xadesXml);
      aktuellFarskapserklaering.setMeldingsidSkatt(getUnikId(aktuellFarskapserklaering.getDokument().getDokumentinnhold().getInnhold(),
          aktuellFarskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()));
      if (farskapsportalEgenskaper.getBrukernotifikasjon().isSkruddPaa()) {
        // Slette fars oppgave for signering på DittNav
        brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(aktuellFarskapserklaering.getId(),
            aktuellFarskapserklaering.getFar().getFoedselsnummer());
        // Informere foreldrene om gjennomført signering og tilgjengelig farskapserklæring
        brukernotifikasjonConsumer.informereForeldreOmTilgjengeligFarskapserklaering(aktuellFarskapserklaering.getFar().getFoedselsnummer(),
            aktuellFarskapserklaering.getMor().getFoedselsnummer());
      }
    }
    return null;
  }

  @Nullable
  private Farskapserklaering oppdatereSigneringsinfoForMor(DokumentStatusDto dokumentStatusDto, Farskapserklaering aktuellFarskapserklaering,
      SignaturDto signatur) {

    haandetereStatusFeilet(dokumentStatusDto, aktuellFarskapserklaering, MOR);

    if (!dokumentStatusDto.getStatusSignering().equals(StatusSignering.PAAGAAR)) {
      aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().setStatusSignering(dokumentStatusDto.getStatusSignering().toString());
      return aktuellFarskapserklaering;
    }

    aktuellFarskapserklaering.getDokument().setPadesUrl(dokumentStatusDto.getPadeslenke().toString());
    aktuellFarskapserklaering.getDokument().setBekreftelsesUrl(dokumentStatusDto.getBekreftelseslenke().toString());

    if (signatur.isHarSignert() && aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() == null) {
      aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(signatur.getTidspunktForStatus());
      aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesUrl(signatur.getXadeslenke().toString());
      var signertDokument = difiESignaturConsumer.henteSignertDokument(dokumentStatusDto.getPadeslenke());
      aktuellFarskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold(signertDokument).build());
      var xadesXml = difiESignaturConsumer.henteXadesXml(signatur.getXadeslenke());
      aktuellFarskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml(xadesXml);
      if (farskapsportalEgenskaper.getBrukernotifikasjon().isSkruddPaa()) {
        brukernotifikasjonConsumer.oppretteOppgaveTilFarOmSignering(aktuellFarskapserklaering.getId(),
            aktuellFarskapserklaering.getFar().getFoedselsnummer());
      }
    }
    return null;
  }

  /**
   * Verifiserer at riktig signeringsstatus er kjent. Oppdaterer status dersom status i lokal database avviker fra status hos Posten. Status vil
   * normalt oppdateres etter brukers redirect fra esigneringsløsningen med på følgende kall til redirect-endepunktet til farskapsportal-api.
   **/
  private Set<Farskapserklaering> oppdatereSigneringsstatusHvisEndret(Set<Farskapserklaering> farskapserklaeringer) {

    var prosesserteFarskapserklaeringer = new HashSet<Farskapserklaering>();

    for (Farskapserklaering farskapserklaering : farskapserklaeringer) {
      prosesserteFarskapserklaeringer.add(oppdatereFarskapserklaeringHvisAktuelt(farskapserklaering));
    }

    return prosesserteFarskapserklaeringer;
  }

  private void haandetereStatusFeilet(DokumentStatusDto dokumentStatusDto, Farskapserklaering farskapserklaering, Rolle rolle) {
    if (dokumentStatusDto.getStatusSignering().equals(StatusSignering.FEILET)) {
      farskapserklaering.getDokument().getSigneringsinformasjonMor().setStatusSignering(dokumentStatusDto.getStatusSignering().toString());
      farskapserklaering.setDeaktivert(LocalDateTime.now());

      if (rolle.equals(FAR)) {
        farskapserklaering.getDokument().getSigneringsinformasjonFar().setStatusSignering(dokumentStatusDto.getStatusSignering().toString());
        if (farskapsportalEgenskaper.getBrukernotifikasjon().isSkruddPaa()) {
          brukernotifikasjonConsumer.varsleMorOmAvbruttSignering(farskapserklaering.getMor().getFoedselsnummer());
        }
      }

      throw new EsigneringStatusFeiletException(Feilkode.ESIGNERING_STATUS_FEILET, farskapserklaering);
    }
  }

  private Farskapserklaering oppdatereFarskapserklaeringHvisAktuelt(Farskapserklaering farskapserklaering) {

    var morHarSignert = farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt() != null;
    var farHarSignert = farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() != null;

    // Henter ikke oppdatert status dersom mor har signert og far ikke har forsøkt å signere, eller både mor og far har signert (implisitt dersom
    // far har signert)
    if (morHarSignert && farskapserklaering.getFarBorSammenMedMor() == null || farHarSignert) {
      return farskapserklaering;
    }

    var oppdatertStatus = difiESignaturConsumer.henteOppdatertStatusPaaSigneringsjobbHvisEndringer(
        farskapserklaering.getId(),
        farskapserklaering.getDokument().getDokumentinnhold().getInnhold(),
        farskapserklaering.getMor().getFoedselsnummer(),
        farskapserklaering.getFar().getFoedselsnummer());

    if (oppdatertStatus.isPresent()) {
      var dokumentStatusDto = oppdatertStatus.get();
      var statusSignering = dokumentStatusDto.getStatusSignering();

      if (StatusSignering.SUKSESS.equals(statusSignering)) {

        var o = oppdatereSigneringsinfo(Optional.empty(), dokumentStatusDto, farskapserklaering);

        return persistenceService.oppdatereFarskapserklaering(o);
      }
    }
    return farskapserklaering;
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

  private ForelderDto oppretteForelderDto(String foedseslnummer) {
    var navnDto = personopplysningService.henteNavn(foedseslnummer);
    var foedselsdato = personopplysningService.henteFoedselsdato(foedseslnummer);
    return ForelderDto.builder()
        .foedselsnummer(foedseslnummer)
        .foedselsdato(foedselsdato)
        .fornavn(navnDto.getFornavn())
        .mellomnavn(navnDto.getMellomnavn())
        .etternavn(navnDto.getEtternavn())
        .build();
  }

  private BarnDto oppretteBarnDto(OppretteFarskapserklaeringRequest request) {
    var foedselsnummer = request.getBarn().getFoedselsnummer();
    if (foedselsnummer != null && !foedselsnummer.isBlank()) {
      var foedselsdato = personopplysningService.henteFoedselsdato(foedselsnummer);
      var foedested = personopplysningService.henteFoedested(request.getBarn().getFoedselsnummer());
      return BarnDto.builder().foedested(foedested).foedselsdato(foedselsdato).foedselsnummer(foedselsnummer).build();
    } else {
      return BarnDto.builder().termindato(request.getBarn().getTermindato()).build();
    }
  }

  private void antallsbegrensetKontrollAvNavnOgNummerPaaFar(String fnrMor, KontrollerePersonopplysningerRequest request) {
    var statusKontrollereFar = persistenceService.henteStatusKontrollereFar(fnrMor);
    if (statusKontrollereFar.isEmpty() || farskapsportalEgenskaper.getKontrollFarMaksAntallForsoek() > statusKontrollereFar.get()
        .getAntallFeiledeForsoek()) {
      kontrollereNavnOgNummerFar(fnrMor, request);
    } else {
      berikeOgKasteFeilNavnOppgittException(fnrMor, new FeilNavnOppgittException(Feilkode.MAKS_ANTALL_FORSOEK));
    }
  }

  private void kontrollereNavnOgNummerFar(String fnrMor, KontrollerePersonopplysningerRequest request) {
    try {
      validereOppgittNavnFar(request.getFoedselsnummer(), request.getNavn());
    } catch (FeilNavnOppgittException e) {
      berikeOgKasteFeilNavnOppgittException(fnrMor, e);
    }
  }

  private void berikeOgKasteFeilNavnOppgittException(String fnrMor, FeilNavnOppgittException e) {
    var statusKontrollereFarDto = mapper
        .toDto(persistenceService.oppdatereStatusKontrollereFar(fnrMor, farskapsportalEgenskaper.getKontrollFarForsoekFornyesEtterAntallDager()));
    e.setStatusKontrollereFarDto(Optional.of(statusKontrollereFarDto));
    var resterendeAntallForsoek = farskapsportalEgenskaper.getKontrollFarMaksAntallForsoek() - statusKontrollereFarDto.getAntallFeiledeForsoek();
    resterendeAntallForsoek = resterendeAntallForsoek < 0 ? 0 : resterendeAntallForsoek;
    statusKontrollereFarDto.setAntallResterendeForsoek(resterendeAntallForsoek);
    throw e;
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

    // Far kan ikke være død
    validereAtForelderIkkeErDoed(foedselsnummer);

    // Far må være myndig (dvs er over 18 år og ingen verge)
    validereAtForelderErMyndig(foedselsnummer);

    var farsForelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

    // Far må ha foreldrerolle FAR eller MOR_ELLER_FAR
    if (!(Forelderrolle.FAR.equals(farsForelderrolle) || Forelderrolle.MOR_ELLER_FAR.equals(farsForelderrolle))) {
      throw new ValideringException(Feilkode.FEIL_ROLLE_FAR);
    }

    // Far skal ikke være registrert med dnummer.
    validereAtPersonHarAktivtFoedselsnummer(foedselsnummer, FAR);
  }

  private void validereAtPersonHarAktivtFoedselsnummer(String foedselsnummer, Rolle rolle) {
    var folkeregisteridentifikatorDto = personopplysningService.henteFolkeregisteridentifikator(foedselsnummer);
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
    if (!personopplysningService.erMyndig(foedselsnummer)) {
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
    validereAtMorOgFarErForskjelligePersoner(fnrMor, request.getOpplysningerOmFar().getFoedselsnummer());
    // Validere at termindato er innenfor gyldig intervall dersom barn ikke er født
    termindatoErGyldig(request.getBarn());
    // Sjekke at ny farskapserklæring ikke kommmer i konflikt med eksisterende
    persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(fnrMor, request.getOpplysningerOmFar().getFoedselsnummer(),
        BarnDto.builder().termindato(request.getBarn().getTermindato()).foedselsnummer(request.getBarn().getFoedselsnummer()).build());
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
      Validate.isTrue(foedeland != null && personopplysningService.henteFoedeland(fnrNyfoedt).equalsIgnoreCase(KODE_LAND_NORGE));
    } catch (IllegalArgumentException iae) {
      log.warn("Barn er født utenfor Norge!");
      throw new ValideringException(Feilkode.BARN_FOEDT_UTENFOR_NORGE);
    }
  }

  private void validereAlderNyfoedt(String fnrOppgittBarn) {
    var foedselsdato = personopplysningService.henteFoedselsdato(fnrOppgittBarn);
    if (!LocalDate.now().minusMonths(farskapsportalEgenskaper.getMaksAntallMaanederEtterFoedsel()).isBefore(foedselsdato)) {
      throw new ValideringException(Feilkode.NYFODT_ER_FOR_GAMMEL);
    }
  }

  private void validereRelasjonerNyfoedt(String fnrMor, String fnrOppgittBarn) {

    if (fnrOppgittBarn == null || fnrOppgittBarn.length() < 1) {
      log.info("Barnet er ikke oppgitt med fødselsnummer");
      return;
    }

    log.info("Validerer at nyfødt barn er relatert til mor, samt har ingen registrert far.");
    var registrerteNyfoedteUtenFar = personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

    registrerteNyfoedteUtenFar.stream().findFirst().orElseThrow(() -> new ValideringException(Feilkode.INGEN_NYFOEDTE_UTEN_FAR));

    registrerteNyfoedteUtenFar.stream().filter(Objects::nonNull).filter(fnrBarn -> fnrBarn.equals(fnrOppgittBarn)).collect(Collectors.toSet())
        .stream().findAny().orElseThrow(() -> new ValideringException(Feilkode.BARN_MANGLER_RELASJON_TIL_MOR));
  }

  private void validereAtMorOgFarErForskjelligePersoner(String fnrMor, String fnrFar) {
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

    log.info("Henter dokumentstatus fra Posten.");

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
}
