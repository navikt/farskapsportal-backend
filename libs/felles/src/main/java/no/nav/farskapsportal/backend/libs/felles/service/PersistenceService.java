package no.nav.farskapsportal.backend.libs.felles.service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.dto.BarnDto;
import no.nav.farskapsportal.backend.libs.entity.*;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.farskapsportal.backend.libs.felles.exception.FeilIDatagrunnlagException;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
import no.nav.farskapsportal.backend.libs.felles.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.exception.ValideringException;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.*;
import no.nav.farskapsportal.backend.libs.felles.persistence.exception.FantIkkeEntititetException;
import no.nav.farskapsportal.backend.libs.felles.util.Utils;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class PersistenceService {

  private final BucketConsumer bucketConsumer;
  private final OppgavebestillingDao oppgavebestillingDao;
  private final FarskapserklaeringDao farskapserklaeringDao;
  private final BarnDao barnDao;
  private final ForelderDao forelderDao;
  private final StatusKontrollereFarDao statusKontrollereFarDao;
  private final MeldingsloggDao meldingsloggDao;
  private final ModelMapper modelMapper;

  public Forelder henteForelder(int id) {
    return forelderDao
        .findById(id)
        .orElseThrow(
            () ->
                new FantIkkeEntititetException(
                    String.format("Fant ingen forelder med id %d i databasen", id)));
  }

  public Optional<Forelder> henteForelderForIdent(String ident) {
    return forelderDao.henteForelderMedFnr(ident);
  }

  public Farskapserklaering oppdatereFarskapserklaering(Farskapserklaering farskapserklaering) {
    if (farskapserklaeringDao.findById(farskapserklaering.getId()).isEmpty()) {
      throw new InternFeilException(Feilkode.INTERN_FEIL_OPPDATERING_AV_ERKLAERING);
    }

    return farskapserklaeringDao.save(farskapserklaering);
  }

  @Transactional
  public Farskapserklaering lagreNyFarskapserklaering(Farskapserklaering nyFarskapserklaering) {

    ingenKonfliktMedEksisterendeFarskapserklaeringer(
        nyFarskapserklaering.getMor().getFoedselsnummer(),
        nyFarskapserklaering.getFar().getFoedselsnummer(),
        modelMapper.map(nyFarskapserklaering.getBarn(), BarnDto.class));

    var eksisterendeMor =
        forelderDao.henteForelderMedFnr(nyFarskapserklaering.getMor().getFoedselsnummer());
    var eksisterendeFar =
        forelderDao.henteForelderMedFnr(nyFarskapserklaering.getFar().getFoedselsnummer());
    // Barn med fødselsnummer kan kun opptre i én aktiv farskapserklæring. Det kan imidlertid
    // eksistere deaktiverte samtidige farskapserklæringer
    // på et nyfødt barn
    var eksisterendeNyfoedt =
        barnDao.henteBarnMedFnr(nyFarskapserklaering.getBarn().getFoedselsnummer());

    nyFarskapserklaering.setMor(eksisterendeMor.orElseGet(nyFarskapserklaering::getMor));
    nyFarskapserklaering.setFar(eksisterendeFar.orElseGet(nyFarskapserklaering::getFar));
    nyFarskapserklaering.setBarn(eksisterendeNyfoedt.orElseGet(nyFarskapserklaering::getBarn));

    return farskapserklaeringDao.save(nyFarskapserklaering);
  }

  public Set<Farskapserklaering> henteMorsEksisterendeErklaeringer(String fnrMor) {
    return farskapserklaeringDao.henteMorsErklaeringer(fnrMor);
  }

  public Set<Farskapserklaering> henteFarsErklaeringer(String fnrFar) {
    return farskapserklaeringDao.henteFarsErklaeringer(fnrFar);
  }

  public Set<Farskapserklaering> henteFarskapserklaeringerForForelder(String fnrForelder) {
    return farskapserklaeringDao.henteFarskapserklaeringerForForelder(fnrForelder);
  }

  public Set<Integer> henteIdTilFarskapserklaeringerMedAktiveOppgaver() {
    return oppgavebestillingDao.henteIdTilFarskapserklaeringerMedAktiveOppgaver();
  }

  public Optional<Farskapserklaering> henteBarnsEksisterendeErklaering(String fnrBarn) {
    var farskapserklaeringer = farskapserklaeringDao.henteBarnsErklaeringer(fnrBarn);

    if (farskapserklaeringer.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        farskapserklaeringer.stream()
            .filter(Objects::nonNull)
            .collect(
                Utils.toSingletonOrThrow(
                    new FeilIDatagrunnlagException(Feilkode.BARN_HAR_FLERE_ERLAERINGER))));
  }

  @Transactional
  public StatusKontrollereFar oppdatereStatusKontrollereFar(
      String fnrMor,
      String registrertNavnFar,
      String oppgittNavnFar,
      int antallDagerTilForsoekNullstilles,
      int maksAntallFeiledeForsoek) {
    var muligStatusKontrollereFar = statusKontrollereFarDao.henteStatusKontrollereFar(fnrMor);
    var naa = LocalDateTime.now();

    if (muligStatusKontrollereFar.isEmpty()) {
      return lagreNyStatusKontrollereFar(
          fnrMor,
          registrertNavnFar,
          oppgittNavnFar,
          LocalDateTime.now().plusDays(antallDagerTilForsoekNullstilles));
    } else {
      var statusKontrollereFar = muligStatusKontrollereFar.get();
      log.warn(
          "Mor  oppgav feil navn på far. Legger til nytt innslag i statusKontrollereFar-tabellen for id {}",
          statusKontrollereFar.getId());
      if (statusKontrollereFar.getTidspunktForNullstilling().isBefore(naa)) {
        statusKontrollereFar.setAntallFeiledeForsoek(1);
        statusKontrollereFar.setRegistrertNavnFar(registrertNavnFar);
        statusKontrollereFar.setOppgittNavnFar(oppgittNavnFar);
        statusKontrollereFar.setTidspunktForNullstilling(
            LocalDateTime.now().plusDays(antallDagerTilForsoekNullstilles));
      } else if (statusKontrollereFar.getAntallFeiledeForsoek() < maksAntallFeiledeForsoek) {
        var antallFeiledeForsoek = statusKontrollereFar.getAntallFeiledeForsoek();
        statusKontrollereFar.setRegistrertNavnFar(registrertNavnFar);
        statusKontrollereFar.setOppgittNavnFar(oppgittNavnFar);
        statusKontrollereFar.setAntallFeiledeForsoek(++antallFeiledeForsoek);
        statusKontrollereFar.setRegistrertNavnFar(registrertNavnFar);
        statusKontrollereFar.setOppgittNavnFar(oppgittNavnFar);
      }
      return statusKontrollereFar;
    }
  }

  public Optional<StatusKontrollereFar> henteStatusKontrollereFar(String fnrMor) {
    return statusKontrollereFarDao.henteStatusKontrollereFar(fnrMor);
  }

  public Farskapserklaering henteFarskapserklaeringForId(int idFarskapserklaering) {
    var farskapserklaering = farskapserklaeringDao.findById(idFarskapserklaering);
    if (farskapserklaering.isPresent() && farskapserklaering.get().getDeaktivert() == null) {
      return farskapserklaering.get();
    }
    log.error("Fant ikke farskapserklæring med id {} i databasen", idFarskapserklaering);
    throw new RessursIkkeFunnetException(Feilkode.FANT_IKKE_FARSKAPSERKLAERING);
  }

  public Farskapserklaering henteDeaktivertFarskapserklaering(int idFarskapserklaering) {
    var farskapserklaering = farskapserklaeringDao.findById(idFarskapserklaering);
    if (farskapserklaering.isPresent() && farskapserklaering.get().getDeaktivert() != null) {
      return farskapserklaering.get();
    }
    log.error("Fant ikke deaktivert farskapserklæring med id {} i databasen", idFarskapserklaering);
    throw new RessursIkkeFunnetException(Feilkode.FANT_IKKE_FARSKAPSERKLAERING);
  }

  public void ingenKonfliktMedEksisterendeFarskapserklaeringer(
      String fnrMor, String fnrFar, BarnDto barnDto) {

    var barnErOppgittMedFoedselsnummer =
        barnDto.getFoedselsnummer() != null && barnDto.getFoedselsnummer().length() > 10;

    // Hente eventuelle eksisterende farskapserklaeringer for mor
    var morsEksisterendeErklaeringer = henteMorsEksisterendeErklaeringer(fnrMor);

    // Hente eventuell eksisterende farskapserklaering for barn, hvis barn oppgitt med fnr
    var barnsEksisterendeErklaering =
        barnErOppgittMedFoedselsnummer
            ? henteBarnsEksisterendeErklaering(barnDto.getFoedselsnummer())
            : Optional.empty();

    if (!barnErOppgittMedFoedselsnummer && !morsEksisterendeErklaeringer.isEmpty()) {
      throw new ValideringException(Feilkode.ERKLAERING_EKSISTERER_MOR);
    }

    if (barnErOppgittMedFoedselsnummer && !morsEksisterendeErklaeringer.isEmpty()) {
      farForskjelligFraFarIEksisterendeFarskapserklaeringForNyfoedt(
          fnrFar, morsEksisterendeErklaeringer);
    }

    if (barnsEksisterendeErklaering.isPresent()) {
      throw new ValideringException(Feilkode.ERKLAERING_EKSISTERER_BARN);
    }
  }

  public Set<Integer> henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt() {
    return farskapserklaeringDao.henteFarskapserklaeringerErKlareForOverfoeringTilSkatt();
  }

  @Transactional
  public boolean deaktivereFarskapserklaering(int idFarskapserklaering) {
    var farskapserklaering = farskapserklaeringDao.findById(idFarskapserklaering);
    if (farskapserklaering.isPresent()) {
      log.info("Deaktiverer farskapserklæring med id {} ", idFarskapserklaering);
      farskapserklaering.get().setDeaktivert(LocalDateTime.now());
      return true;
    } else {
      log.error(
          "Farskapserklæring med id {} ble ikke funnet i databasen, og kunne av den grunn ikke deaktiveres.",
          idFarskapserklaering);
      throw new IllegalStateException("Farskapserklæring ikke funnet");
    }
  }

  @Transactional
  public void sletteDokumentinnhold(int idFarskapserklaering) {
    var farskapserklaering = farskapserklaeringDao.findById(idFarskapserklaering);

    if (farskapserklaering.isPresent()) {
      if (farskapserklaering.get().getDeaktivert() == null) {
        log.error(
            "Kan ikke slette dokument knyttet til aktiv farskapserklæring med id {}",
            idFarskapserklaering);
        throw new IllegalStateException("Farskapserklæringen ikke deaktivert");
      }

      if (erklaeringHarReferanserTilBuckets(farskapserklaering.get())) {

        var padesBlobGcp = farskapserklaering.get().getDokument().getBlobIdGcp();
        if (padesBlobGcp != null) {
          if (bucketConsumer.deleteContentFromBucket(padesBlobGcp)) {
            farskapserklaering.get().getDokument().setBlobIdGcp(null);
          } else {
            validerAtBlobIkkeEksisterer(padesBlobGcp);
          }
        }

        var xadesMorBlobGcp =
            farskapserklaering.get().getDokument().getSigneringsinformasjonMor().getBlobIdGcp();
        if (xadesMorBlobGcp != null) {
          if (bucketConsumer.deleteContentFromBucket(xadesMorBlobGcp)) {
            farskapserklaering.get().getDokument().getSigneringsinformasjonMor().setBlobIdGcp(null);
          } else {
            validerAtBlobIkkeEksisterer(xadesMorBlobGcp);
          }
        }

        var xadesFarBlobGcp =
            farskapserklaering.get().getDokument().getSigneringsinformasjonFar().getBlobIdGcp();
        if (xadesFarBlobGcp != null) {
          if (bucketConsumer.deleteContentFromBucket(xadesFarBlobGcp)) {
            farskapserklaering.get().getDokument().getSigneringsinformasjonFar().setBlobIdGcp(null);
          } else {
            validerAtBlobIkkeEksisterer(xadesMorBlobGcp);
          }
        }
      }

      // TODO: Fjerne etter fullført bucket-migrering
      sletteDokumentinnholdFraDatabase(farskapserklaering.get());

      farskapserklaering.get().setDokumenterSlettet(LocalDateTime.now());
    } else {
      log.error("Fant ikke deaktivert farskapserklæring med id {}", idFarskapserklaering);
      throw new IllegalStateException("Fant ikke farskapserklæring");
    }
  }

  // TODO: Fjernes etter fullført bucket-migrering
  @Deprecated
  private void sletteDokumentinnholdFraDatabase(Farskapserklaering farskapserklaering) {
    farskapserklaering.getDokument().getDokumentinnhold().setInnhold(null);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml(null);
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml(null);
  }

  public Oppgavebestilling lagreNyOppgavebestilling(int idFarskapserklaering, String eventId) {
    var farskapserklaering = henteFarskapserklaeringForId(idFarskapserklaering);

    var oppgavebestilling =
        Oppgavebestilling.builder()
            .farskapserklaering(farskapserklaering)
            .forelder(farskapserklaering.getFar())
            .eventId(eventId)
            .opprettet(LocalDateTime.now())
            .build();
    return oppgavebestillingDao.save(oppgavebestilling);
  }

  public Set<Oppgavebestilling> henteAktiveOppgaverTilForelderIFarskapserklaering(
      int idFarskapserklaering, Forelder forelder) {
    return oppgavebestillingDao.henteAktiveOppgaver(
        idFarskapserklaering, forelder.getFoedselsnummer());
  }

  public Set<Integer> henteIdTilAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag(
      LocalDateTime utloepstidspunkt) {
    return farskapserklaeringDao.henteIdTilAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag(
        utloepstidspunkt);
  }

  public Set<Integer> henteIdTilOversendteFarskapserklaeringerSomErKlarForDeaktivering(
      LocalDateTime tidspunktOversendt) {
    return farskapserklaeringDao.henteIdTilOversendteFarskapserklaeringerSomSkalDeaktiveres(
        tidspunktOversendt, tidspunktOversendt.toLocalDate());
  }

  public Set<Integer> henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoFar(
      LocalDateTime farSendtTilSigneringFoer) {
    return farskapserklaeringDao.henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoForFar(
        farSendtTilSigneringFoer);
  }

  public Set<Integer> henteIdTilFarskapserklaeringerSomManglerMorsSignatur(
      LocalDateTime morSendtTilSigneringFoer) {
    return farskapserklaeringDao.henteIdTilFarskapserklaeringerSomManglerMorsSignatur(
        morSendtTilSigneringFoer);
  }

  // TODO: Oppdatere uttrekk etter fullført bucket-migrering
  public Set<Integer> henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(
      LocalDateTime sendtTilSkattFoer, LocalDateTime deaktivertFoer) {
    return farskapserklaeringDao.henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(
        sendtTilSkattFoer, deaktivertFoer);
  }

  @Transactional
  public void setteOppgaveTilFerdigstilt(String eventId) {
    var aktiveOppgaver = oppgavebestillingDao.henteOppgavebestilling(eventId);

    if (aktiveOppgaver.isPresent()) {
      aktiveOppgaver.get().setFerdigstilt(LocalDateTime.now());
    } else {
      log.warn(
          "Fant ingen oppgavebestilling med eventId {}, ferdigstiltstatus ble ikke satt!", eventId);
    }
  }

  public void oppdatereMeldingslogg(LocalDateTime tidspunktForOverfoering, String meldingsidSkatt) {
    var nyttInnslag =
        Meldingslogg.builder()
            .tidspunktForOversendelse(tidspunktForOverfoering)
            .meldingsidSkatt(meldingsidSkatt)
            .build();

    meldingsloggDao.save(nyttInnslag);
  }

  private boolean erklaeringHarReferanserTilBuckets(Farskapserklaering farskapserklaering) {
    return farskapserklaering.getDokument().getBlobIdGcp() != null
        || farskapserklaering.getDokument().getSigneringsinformasjonMor().getBlobIdGcp() != null
        || farskapserklaering.getDokument().getSigneringsinformasjonFar().getBlobIdGcp() != null;
  }

  private void validerAtBlobIkkeEksisterer(BlobIdGcp blob) {
    var padesBlob = bucketConsumer.getExistingBlobIdGcp(blob.getBucket(), blob.getName());
    if (padesBlob.isPresent()) {
      log.error(
          "Fikk ikke slettet dokument blob {} fra bucket {} (blobIdGcp id: {})",
          blob.getName(),
          blob.getBucket(),
          blob.getId());
      throw new InternFeilException(Feilkode.RYDDEJOBB_FEILET);
    }
  }

  private void farForskjelligFraFarIEksisterendeFarskapserklaeringForNyfoedt(
      String fnrFar, Set<Farskapserklaering> morsEksisterendeFarskapserklaeringer) {
    for (Farskapserklaering farskapserklaering : morsEksisterendeFarskapserklaeringer) {
      if (!fnrFar.equals(farskapserklaering.getFar().getFoedselsnummer())) {
        throw new ValideringException(Feilkode.FORSKJELLIGE_FEDRE);
      }
    }
  }

  private StatusKontrollereFar lagreNyStatusKontrollereFar(
      String fnrMor,
      String registrertNavnFar,
      String oppgittNavnFar,
      LocalDateTime tidspunktForNullstilling) {
    var eksisterendeMor = forelderDao.henteForelderMedFnr(fnrMor);
    var mor =
        eksisterendeMor.orElseGet(
            () -> forelderDao.save(Forelder.builder().foedselsnummer(fnrMor).build()));
    log.warn(
        "Mor med id {}, oppgav feil navn på far. Legger til nytt innslag i statusKontrollereFar-tabellen",
        mor.getId());
    var statusKontrollereFar =
        StatusKontrollereFar.builder()
            .mor(mor)
            .registrertNavnFar(registrertNavnFar)
            .oppgittNavnFar(oppgittNavnFar)
            .tidspunktForNullstilling(tidspunktForNullstilling)
            .antallFeiledeForsoek(1)
            .build();
    return statusKontrollereFarDao.save(statusKontrollereFar);
  }
}
