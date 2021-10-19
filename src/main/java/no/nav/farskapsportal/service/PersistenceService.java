package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.util.Utils.toSingletonOrThrow;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.FeilIDatagrunnlagException;
import no.nav.farskapsportal.exception.InternFeilException;
import no.nav.farskapsportal.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.exception.ValideringException;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.Meldingslogg;
import no.nav.farskapsportal.persistence.entity.Oppgavebestilling;
import no.nav.farskapsportal.persistence.entity.StatusKontrollereFar;
import no.nav.farskapsportal.persistence.exception.FantIkkeEntititetException;
import no.nav.farskapsportal.util.Mapper;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class PersistenceService {

  private final OppgavebestillingDao oppgavebestillingDao;

  private final PersonopplysningService personopplysningService;

  private final FarskapserklaeringDao farskapserklaeringDao;

  private final BarnDao barnDao;

  private final ForelderDao forelderDao;

  private final StatusKontrollereFarDao statusKontrollereFarDao;

  private final MeldingsloggDao meldingsloggDao;

  private final Mapper mapper;

  public BarnDto henteBarn(int id) {
    var barn = barnDao.findById(id).orElseThrow(() -> new FantIkkeEntititetException(String.format("Fant ikke barn med id %d i databasen", id)));
    return mapper.toDto(barn);
  }

  public Forelder henteForelder(int id) {
    return forelderDao.findById(id)
        .orElseThrow(() -> new FantIkkeEntititetException(String.format("Fant ingen forelder med id %d i databasen", id)));
  }

  public Farskapserklaering oppdatereFarskapserklaering(Farskapserklaering farskapserklaering) {
    if (farskapserklaeringDao.findById(farskapserklaering.getId()).isEmpty()) {
      throw new InternFeilException(Feilkode.INTERN_FEIL_OPPDATERING_AV_ERKLAERING);
    }

    return farskapserklaeringDao.save(farskapserklaering);
  }

  @Transactional
  public Farskapserklaering lagreNyFarskapserklaering(Farskapserklaering nyFarskapserklaering) {

    ingenKonfliktMedEksisterendeFarskapserklaeringer(nyFarskapserklaering.getMor().getFoedselsnummer(),
        nyFarskapserklaering.getFar().getFoedselsnummer(), mapper.toDto(nyFarskapserklaering.getBarn()));

    var eksisterendeMor = forelderDao.henteForelderMedFnr(nyFarskapserklaering.getMor().getFoedselsnummer());
    var eksisterendeFar = forelderDao.henteForelderMedFnr(nyFarskapserklaering.getFar().getFoedselsnummer());
    // Barn med fødselsnummer kan kun opptre i én aktiv farskapserklæring. Det kan imidlertid eksistere deaktiverte samtidige farskapserklæringer
    // på et nyfødt barn
    var eksisterendeNyfoedt = barnDao.henteBarnMedFnr(nyFarskapserklaering.getBarn().getFoedselsnummer());

    nyFarskapserklaering.setMor(eksisterendeMor.orElseGet(nyFarskapserklaering::getMor));
    nyFarskapserklaering.setFar(eksisterendeFar.orElseGet(nyFarskapserklaering::getFar));
    nyFarskapserklaering.setBarn(eksisterendeNyfoedt.orElseGet(nyFarskapserklaering::getBarn));

    return farskapserklaeringDao.save(nyFarskapserklaering);
  }

  public Set<Farskapserklaering> henteMorsEksisterendeErklaeringer(String fnrMor) {
    return bareAktive(farskapserklaeringDao.henteMorsErklaeringer(fnrMor));
  }

  public Set<Farskapserklaering> henteFarsErklaeringer(String fnrFar) {
    return bareAktive(farskapserklaeringDao.henteFarsErklaeringer(fnrFar));
  }

  public Set<Farskapserklaering> henteFarskapserklaeringerForForelder(String fnrForelder) {
    return bareAktive(farskapserklaeringDao.henteFarskapserklaeringerForForelder(fnrForelder));
  }

  public Optional<Farskapserklaering> henteBarnsEksisterendeErklaering(String fnrBarn) {
    var farskapserklaeringer = bareAktive(farskapserklaeringDao.henteBarnsErklaeringer(fnrBarn));

    if (farskapserklaeringer.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(farskapserklaeringer.stream().filter(Objects::nonNull)
        .collect(toSingletonOrThrow(new FeilIDatagrunnlagException(Feilkode.BARN_HAR_FLERE_ERLAERINGER))));
  }

  @Transactional
  public Set<Farskapserklaering> henteFarskapserklaeringerEtterRedirect(String fnrForelder, Forelderrolle forelderrolle, KjoennType gjeldendeKjoenn) {
    switch (forelderrolle) {
      case MOR:
        return bareAktive(farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder));
      case FAR:
        return henteFarsErklaeringer(fnrForelder);
      case MOR_ELLER_FAR:
        if (KjoennType.KVINNE.equals(gjeldendeKjoenn)) {
          return bareAktive(farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder));
        } else if (KjoennType.MANN.equals(gjeldendeKjoenn)) {
          return bareAktive(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
        }
      default:
        throw new ValideringException(Feilkode.FEIL_ROLLE);
    }
  }

  @Transactional
  public StatusKontrollereFar oppdatereStatusKontrollereFar(String fnrMor, String registrertNavnFar, String oppgittNavnFar,
      int antallDagerTilForsoekNullstilles, int maksAntallFeiledeForsoek) {
    var muligStatusKontrollereFar = statusKontrollereFarDao.henteStatusKontrollereFar(fnrMor);
    var naa = LocalDateTime.now();

    if (muligStatusKontrollereFar.isEmpty()) {
      return lagreNyStatusKontrollereFar(fnrMor, registrertNavnFar, oppgittNavnFar, LocalDateTime.now().plusDays(antallDagerTilForsoekNullstilles));
    } else {
      var statusKontrollereFar = muligStatusKontrollereFar.get();
      log.warn("Mor  oppgav feil navn på far. Legger til nytt innslag i statusKontrollereFar-tabellen for id {}", statusKontrollereFar.getId());
      if (statusKontrollereFar.getTidspunktForNullstilling().isBefore(naa)) {
        statusKontrollereFar.setAntallFeiledeForsoek(1);
        statusKontrollereFar.setRegistrertNavnFar(registrertNavnFar);
        statusKontrollereFar.setOppgittNavnFar(oppgittNavnFar);
        statusKontrollereFar.setTidspunktForNullstilling(LocalDateTime.now().plusDays(antallDagerTilForsoekNullstilles));
      } else if (statusKontrollereFar.getAntallFeiledeForsoek() < maksAntallFeiledeForsoek) {
        var antallFeiledeForsoek = statusKontrollereFar.getAntallFeiledeForsoek();
        statusKontrollereFar.setRegistrertNavnFar(registrertNavnFar);
        statusKontrollereFar.setOppgittNavnFar(oppgittNavnFar);
        statusKontrollereFar.setAntallFeiledeForsoek(++antallFeiledeForsoek);
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
    throw new RessursIkkeFunnetException(Feilkode.FANT_IKKE_FARSKAPSERKLAERING);
  }

  public void ingenKonfliktMedEksisterendeFarskapserklaeringer(String fnrMor, String fnrFar, BarnDto barnDto) {

    var barnErOppgittMedFoedselsnummer = barnDto.getFoedselsnummer() != null && barnDto.getFoedselsnummer().length() > 10;

    // Hente eventuelle eksisterende farskapserklaeringer for mor
    var morsEksisterendeErklaeringer = henteMorsEksisterendeErklaeringer(fnrMor);

    // Hente eventuell eksisterende farskapserklaering for barn, hvis barn oppgitt med fnr
    var barnsEksisterendeErklaering =
        barnErOppgittMedFoedselsnummer ? henteBarnsEksisterendeErklaering(barnDto.getFoedselsnummer()) : Optional.empty();

    if (!barnErOppgittMedFoedselsnummer && !morsEksisterendeErklaeringer.isEmpty()) {
      throw new ValideringException(Feilkode.ERKLAERING_EKSISTERER_MOR);
    }

    if (barnErOppgittMedFoedselsnummer && !morsEksisterendeErklaeringer.isEmpty()) {
      farForskjelligFraFarIEksisterendeFarskapserklaeringForNyfoedt(fnrFar, morsEksisterendeErklaeringer);
    }

    if (barnsEksisterendeErklaering.isPresent()) {
      throw new ValideringException(Feilkode.ERKLAERING_EKSISTERER_BARN);
    }
  }

  public Set<Farskapserklaering> henteFarskapserklaeringerHvorFarIkkeBorSammenMedMorOgErSendtTilSkattMenIkkeJoark() {
    return bareAktive(farskapserklaeringDao.henteFarskapserklaeringerSomTidligereErForsoektSendtTilJoark());
  }

  public Set<Farskapserklaering> henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt() {
    return bareAktive(farskapserklaeringDao.henteFarskapserklaeringerErKlareForOverfoeringTilSkatt());
  }

  public Set<Farskapserklaering> henteFarskapserklaeringerSomVenterPaaFarsSignatur() {
    return bareAktive(farskapserklaeringDao.henteFarskapserklaeringerSomVenterPaaFarsSignatur());
  }

  @Transactional
  public void deaktivereFarskapserklaering(int idFarskapserklaering) {
    var farskapserklaering = farskapserklaeringDao.findById(idFarskapserklaering);
    if (farskapserklaering.isPresent()) {
      farskapserklaering.get().setDeaktivert(LocalDateTime.now());
    } else {
      throw new InternFeilException(Feilkode.FANT_IKKE_FARSKAPSERKLAERING);
    }
  }

  public Oppgavebestilling lagreNyOppgavebestilling(int idFarskapserklaering, String eventId) {
    var farskapserklaering = henteFarskapserklaeringForId(idFarskapserklaering);

    var oppgavebestilling = Oppgavebestilling.builder()
        .farskapserklaering(farskapserklaering)
        .forelder(farskapserklaering.getFar())
        .eventId(eventId)
        .opprettet(LocalDateTime.now()).build();
    return oppgavebestillingDao.save(oppgavebestilling);
  }

  public Set<Oppgavebestilling> henteAktiveOppgaverTilForelderIFarskapserklaering(int idFarskapserklaering, Forelder forelder) {
    return oppgavebestillingDao.henteAktiveOppgaverIFarskapserklaering(idFarskapserklaering, forelder.getFoedselsnummer());
  }

  @Transactional
  public void setteOppgaveTilFerdigstilt(String eventId) {
    var aktiveOppgaver = oppgavebestillingDao.henteOppgave(eventId);

    if (aktiveOppgaver.isPresent()) {
      aktiveOppgaver.get().setFerdigstilt(LocalDateTime.now());
    } else {
      log.warn("Fant ingen oppgavebestilling med eventId {}, ferdigstiltstatus ble ikke satt!", eventId);
    }
  }

  public void oppdatereMeldingslogg(LocalDateTime tidspunktForOverfoering, String meldingsidSkatt) {
    var nyttInnslag = Meldingslogg.builder().tidspunktForOversendelse(tidspunktForOverfoering).meldingsidSkatt(meldingsidSkatt).build();
    meldingsloggDao.save(nyttInnslag);
  }

  private Set<Farskapserklaering> bareAktive(Set<Farskapserklaering> farskapserklaeringer) {
    return farskapserklaeringer.stream().filter(fe -> fe.getDeaktivert() == null).collect(Collectors.toSet());
  }

  private void farForskjelligFraFarIEksisterendeFarskapserklaeringForNyfoedt(String fnrFar,
      Set<Farskapserklaering> morsEksisterendeFarskapserklaeringer) {
    for (Farskapserklaering farskapserklaering : morsEksisterendeFarskapserklaeringer) {
      if (!fnrFar.equals(farskapserklaering.getFar().getFoedselsnummer())) {
        throw new ValideringException(Feilkode.FORSKJELLIGE_FEDRE);
      }
    }
  }

  private ForelderDto henteForelder(String fnr) {
    var navnDto = personopplysningService.henteNavn(fnr);
    return ForelderDto.builder().foedselsnummer(fnr).navn(navnDto).build();
  }

  private StatusKontrollereFar lagreNyStatusKontrollereFar(String fnrMor, String registrertNavnFar, String oppgittNavnFar,
      LocalDateTime tidspunktForNullstilling) {
    var eksisterendeMor = forelderDao.henteForelderMedFnr(fnrMor);
    var mor = eksisterendeMor.orElseGet(() -> forelderDao.save(mapper.toEntity(henteForelder(fnrMor))));
    log.warn("Mor med id {}, oppgav feil navn på far. Legger til nytt innslag i statusKontrollereFar-tabellen", mor.getId());
    var statusKontrollereFar = StatusKontrollereFar.builder()
        .mor(mor)
        .registrertNavnFar(registrertNavnFar)
        .oppgittNavnFar(oppgittNavnFar)
        .tidspunktForNullstilling(tidspunktForNullstilling)
        .antallFeiledeForsoek(1)
        .build();
    return statusKontrollereFarDao.save(statusKontrollereFar);
  }
}
