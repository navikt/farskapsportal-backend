package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.util.Utils.toSingletonOrThrow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.config.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.skatt.api.Far;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.FeilIDatagrunnlagException;
<<<<<<< HEAD
import no.nav.farskapsportal.exception.InternFeilException;
=======
>>>>>>> main
import no.nav.farskapsportal.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.exception.ValideringException;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.persistence.entity.Barn;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.Meldingslogg;
import no.nav.farskapsportal.persistence.entity.StatusKontrollereFar;
import no.nav.farskapsportal.persistence.exception.FantIkkeEntititetException;
import no.nav.farskapsportal.util.MappingUtil;
import org.modelmapper.ModelMapper;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class PersistenceService {

  private final PersonopplysningService personopplysningService;

  private final FarskapsportalEgenskaper farskapsportalEgenskaperConfig;

  private final FarskapserklaeringDao farskapserklaeringDao;

  private final BarnDao barnDao;

  private final ForelderDao forelderDao;

  private final StatusKontrollereFarDao kontrollereFarDao;

  private final MeldingsloggDao meldingsloggDao;

  private final MappingUtil mappingUtil;

  public BarnDto henteBarn(int id) {
    var barn = barnDao.findById(id).orElseThrow(() -> new FantIkkeEntititetException(String.format("Fant ikke barn med id %d i databasen", id)));
    return mappingUtil.toDto(barn);
  }

  public Forelder henteForelder(int id) {
    var forelder = forelderDao.findById(id)
        .orElseThrow(() -> new FantIkkeEntititetException(String.format("Fant ingen forelder med id %d i databasen", id)));
    return forelder;
  }

  public Farskapserklaering oppdatereFarskapserklaering(Farskapserklaering farskapserklaering) {
    if (farskapserklaeringDao.findById(farskapserklaering.getId()).isEmpty()) {
      throw new InternFeilException(Feilkode.INTERN_FEIL_OPPDATERING_AV_ERKLAERING);
    }

    return  farskapserklaeringDao.save(farskapserklaering);
  }

  @Transactional
  public Farskapserklaering lagreNyFarskapserklaering(Farskapserklaering nyFarskapserklaering) {

    ingenKonfliktMedEksisterendeFarskapserklaeringer(nyFarskapserklaering.getMor().getFoedselsnummer(),
        nyFarskapserklaering.getFar().getFoedselsnummer(), mappingUtil.toDto(nyFarskapserklaering.getBarn()));

    var eksisterendeMor = forelderDao.henteForelderMedFnr(nyFarskapserklaering.getMor().getFoedselsnummer());
    var eksisterendeFar = forelderDao.henteForelderMedFnr(nyFarskapserklaering.getFar().getFoedselsnummer());

    nyFarskapserklaering.setMor(eksisterendeMor.orElseGet(() -> nyFarskapserklaering.getMor()));
    nyFarskapserklaering.setFar(eksisterendeFar.orElseGet(() -> nyFarskapserklaering.getFar()));

    return farskapserklaeringDao.save(nyFarskapserklaering);
  }

  @Transactional
  public Farskapserklaering lagreNyFarskapserklaering(FarskapserklaeringDto farskapserklaeringDto) {

    ingenKonfliktMedEksisterendeFarskapserklaeringer(farskapserklaeringDto.getMor().getFoedselsnummer(),
        farskapserklaeringDto.getFar().getFoedselsnummer(), farskapserklaeringDto.getBarn());

    var eksisterendeMor = forelderDao.henteForelderMedFnr(farskapserklaeringDto.getMor().getFoedselsnummer());
    var eksisterendeFar = forelderDao.henteForelderMedFnr(farskapserklaeringDto.getFar().getFoedselsnummer());

    var farskapserklaering = Farskapserklaering.builder().mor(eksisterendeMor.orElseGet(() -> mappingUtil.toEntity(farskapserklaeringDto.getMor())))
        .far(eksisterendeFar.orElseGet(() -> mappingUtil.toEntity(farskapserklaeringDto.getFar())))
        .barn(mappingUtil.toEntity(farskapserklaeringDto.getBarn())).dokument(mappingUtil.toEntity(farskapserklaeringDto.getDokument())).build();

    return farskapserklaeringDao.save(farskapserklaering);
  }

  public Set<FarskapserklaeringDto> henteMorsEksisterendeErklaeringer(String fnrMor) {
    var farskapserklaeringer = farskapserklaeringDao.henteMorsErklaeringer(fnrMor);
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
  }

  public Set<FarskapserklaeringDto> henteFarsErklaeringer(String fnrFar) {
    var farskapserklaeringer = farskapserklaeringDao.henteFarsErklaeringer(fnrFar);
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
  }

  public Set<Farskapserklaering> henteFarskapserklaeringerForForelder(String fnrForelder) {
    return farskapserklaeringDao.henteFarskapserklaeringerForForelder(fnrForelder);
  }

  public Optional<FarskapserklaeringDto> henteBarnsEksisterendeErklaering(String fnrBarn) {
    var farskapserklaeringer = farskapserklaeringDao.henteBarnsErklaeringer(fnrBarn);

    if (farskapserklaeringer.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto)
        .collect(toSingletonOrThrow(new FeilIDatagrunnlagException(Feilkode.BARN_HAR_FLERE_ERLAERINGER))));
  }

  @Transactional
  public Set<Farskapserklaering> henteFarskapserklaeringerEtterRedirect(String fnrForelder, Forelderrolle forelderrolle, KjoennType gjeldendeKjoenn) {
    switch (forelderrolle) {
      case MOR:
        return farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder);
      case FAR:
        return farskapserklaeringDao.henteFarskapserklaeringerForFar(fnrForelder);
      case MOR_ELLER_FAR:
        if (KjoennType.KVINNE.equals(gjeldendeKjoenn)) {
          return farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder);
        } else if (KjoennType.MANN.equals(gjeldendeKjoenn)) {
          return farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder);
        }
      default:
        throw new ValideringException(Feilkode.FEIL_ROLLE);
    }
  }

  @Transactional
  public StatusKontrollereFar oppdatereStatusKontrollereFar(String fnrMor, int antallDagerTilForsoekNullstilles) {
    var muligStatusKontrollereFar = kontrollereFarDao.henteStatusKontrollereFar(fnrMor);
    if (muligStatusKontrollereFar.isEmpty()) {
      return lagreNyStatusKontrollereFar(fnrMor);
    } else {
      var statusKontrollereFar = muligStatusKontrollereFar.get();
      var tidspunktNaarAntallForsoekNullstilles = statusKontrollereFar.getTidspunktSisteFeiledeForsoek().plusDays(antallDagerTilForsoekNullstilles);
      var antallFeiledeForsoek =
          statusKontrollereFar.getTidspunktSisteFeiledeForsoek().isBefore(tidspunktNaarAntallForsoekNullstilles) ? statusKontrollereFar
              .getAntallFeiledeForsoek() : 0;
      statusKontrollereFar.setAntallFeiledeForsoek(++antallFeiledeForsoek);
      statusKontrollereFar.setTidspunktSisteFeiledeForsoek(LocalDateTime.now());
      return statusKontrollereFar;
    }
  }

  private StatusKontrollereFar lagreNyStatusKontrollereFar(String fnrMor) {
    var eksisterendeMor = forelderDao.henteForelderMedFnr(fnrMor);
    var mor = eksisterendeMor.isPresent() ? eksisterendeMor.get() : forelderDao.save(mappingUtil.toEntity(getForelder(fnrMor, null)));
    var statusKontrollereFar = StatusKontrollereFar.builder().mor(mor).tidspunktSisteFeiledeForsoek(LocalDateTime.now()).antallFeiledeForsoek(1)
        .build();
    return kontrollereFarDao.save(statusKontrollereFar);
  }

  public Optional<StatusKontrollereFar> henteStatusKontrollereFar(String fnrMor) {
    var statusKontrollereFar = kontrollereFarDao.henteStatusKontrollereFar(fnrMor);
    return statusKontrollereFar;
  }

  public Farskapserklaering henteFarskapserklaeringForId(int idFarskapserklaering) {
    var farskapserklaering = farskapserklaeringDao.findById(idFarskapserklaering);
    if (farskapserklaering.isPresent()) {
      return farskapserklaering.get();
    }
    throw new RessursIkkeFunnetException(Feilkode.FANT_IKKE_FARSKAPSERKLAERING);
  }

  private Set<FarskapserklaeringDto> mapTilDto(Set<Farskapserklaering> farskapserklaeringer) {
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
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

  public Set<Farskapserklaering> henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt() {
    return farskapserklaeringDao.henteFarskapserklaeringerErKlareForOverfoeringTilSkatt();
  }

  public Meldingslogg oppdatereMeldingslogg(LocalDateTime tidspunktForOverfoering, long meldingsidSkatt) {
    var nyttInnslag = Meldingslogg.builder().tidspunktForOversendelse(tidspunktForOverfoering).meldingsidSkatt(meldingsidSkatt).build();
    return  meldingsloggDao.save(nyttInnslag);
  }

  private void farForskjelligFraFarIEksisterendeFarskapserklaeringForNyfoedt(String fnrFar,
      Set<FarskapserklaeringDto> morsEksisterendeFarskapserklaeringer) {
    for (FarskapserklaeringDto farskapserklaering : morsEksisterendeFarskapserklaeringer) {
      if (!fnrFar.equals(farskapserklaering.getFar().getFoedselsnummer())) {
        throw new ValideringException(Feilkode.FORSKJELLIGE_FEDRE);
      }
    }
  }

  @Deprecated
  private Set<FarskapserklaeringDto> henteFarskapserklaeringVedRedirectMorEllerFar(String fnrForelder) {
    var farskapserklaeringer = farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder);
    if (farskapserklaeringer.stream().filter(Objects::nonNull).count() < 1) {
      return mapTilDto(farskapserklaeringer);
    } else {
      return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
    }
  }

  @Deprecated
  @Transactional(readOnly = true)
  public Set<FarskapserklaeringDto> henteAktiveFarskapserklaeringer(String fnrForelder, Forelderrolle forelderrolle, KjoennType gjeldendeKjoenn) {
    switch (forelderrolle) {
      case MOR:
        return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder));
      case FAR:
        return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
      case MOR_ELLER_FAR:
        if (KjoennType.KVINNE.equals(gjeldendeKjoenn)) {
          return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder));
        } else if (KjoennType.MANN.equals(gjeldendeKjoenn)) {
          return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
        }
      default:
        throw new ValideringException(Feilkode.FEIL_ROLLE);
    }
  }

  @Deprecated
  private Optional<Farskapserklaering> henteEksisterendeFarskapserklaeringForUfoedtBarn(FarskapserklaeringDto dto) {

    var nedreGrense = LocalDate.now().plusWeeks(farskapsportalEgenskaperConfig.getMinAntallUkerTilTermindato() - 1);
    var oevreGrense = LocalDate.now().plusWeeks(farskapsportalEgenskaperConfig.getMaksAntallUkerTilTermindato());

    var respons = farskapserklaeringDao
        .henteFarskapserklaeringerForForelder(dto.getMor().getFoedselsnummer(), dto.getFar().getFoedselsnummer(), nedreGrense, oevreGrense);

    return respons.isEmpty() ? Optional.empty() : respons.stream().findFirst();
  }

  private ForelderDto getForelder(String fnr, Forelderrolle rolle) {
    var navn = personopplysningService.henteNavn(fnr);
    return ForelderDto.builder().foedselsnummer(fnr).fornavn(navn.getFornavn()).mellomnavn(navn.getMellomnavn()).etternavn(navn.getEtternavn())
        .build();
  }
}
