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
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.FeilIDatagrunnlagException;
import no.nav.farskapsportal.exception.ValideringException;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.DokumentDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.persistence.entity.Barn;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.StatusKontrollereFar;
import no.nav.farskapsportal.persistence.exception.FantIkkeEntititetException;
import no.nav.farskapsportal.util.MappingUtil;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class PersistenceService {

  private final PersonopplysningService personopplysningService;

  private final FarskapsportalEgenskaper farskapsportalEgenskaperConfig;

  private final FarskapserklaeringDao farskapserklaeringDao;

  private final BarnDao barnDao;

  private final ForelderDao forelderDao;

  private final DokumentDao dokumentDao;

  private final StatusKontrollereFarDao kontrollereFarDao;

  private final MappingUtil mappingUtil;

  public Barn lagreBarn(BarnDto dto) {
    var entity = mappingUtil.toEntity(dto);
    return barnDao.save(entity);
  }

  public BarnDto henteBarn(int id) {
    var barn = barnDao.findById(id).orElseThrow(() -> new FantIkkeEntititetException(String.format("Fant ikke barn med id %d i databasen", id)));
    return mappingUtil.toDto(barn);
  }

  public Forelder lagreForelder(ForelderDto forelderDto) {
    var forelder = mappingUtil.toEntity(forelderDto);
    return forelderDao.save(forelder);
  }

  public ForelderDto henteForelder(int id) {
    var forelder = forelderDao.findById(id)
        .orElseThrow(() -> new FantIkkeEntititetException(String.format("Fant ingen forelder med id %d i databasen", id)));
    return mappingUtil.toDto(forelder);
  }

  public Optional<ForelderDto> henteForelderMedFoedselsnummer(String foedselsnummer) {
    var forelder = forelderDao.henteForelderMedFnr(foedselsnummer);
    if (forelder.isPresent()) {
      return Optional.of(mappingUtil.toDto(forelder.get()));
    }
    return Optional.empty();
  }

  public Dokument lagreDokument(DokumentDto dto) {
    var dokument = mappingUtil.toEntity(dto);
    return dokumentDao.save(dokument);
  }

  @Transactional
  public Farskapserklaering lagreFarskapserklaering(FarskapserklaeringDto dto) {

    ingenKonfliktMedEksisterendeFarskapserklaeringer(dto.getMor().getFoedselsnummer(), dto.getFar().getFoedselsnummer(), dto.getBarn());

    var eksisterendeMor = forelderDao.henteForelderMedFnr(dto.getMor().getFoedselsnummer());
    var eksisterendeFar = forelderDao.henteForelderMedFnr(dto.getFar().getFoedselsnummer());

    var farskapserklaering = Farskapserklaering.builder().mor(eksisterendeMor.orElseGet(() -> mappingUtil.toEntity(dto.getMor())))
        .far(eksisterendeFar.orElseGet(() -> mappingUtil.toEntity(dto.getFar()))).barn(mappingUtil.toEntity(dto.getBarn()))
        .dokument(mappingUtil.toEntity(dto.getDokument())).build();

    return farskapserklaeringDao.save(farskapserklaering);
  }

  public Set<FarskapserklaeringDto> henteFarskapserklaeringer(String foedselsnummer) {
    var farskapserklaeringer = farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(foedselsnummer);
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
  }

  public Set<FarskapserklaeringDto> henteMorsEksisterendeErklaeringer(String fnrMor) {
    var farskapserklaeringer = farskapserklaeringDao.henteMorsErklaeringer(fnrMor);
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
  }

  public Set<FarskapserklaeringDto> henteFarsErklaeringer(String fnrFar) {
    var farskapserklaeringer = farskapserklaeringDao.henteFarsErklaeringer(fnrFar);
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
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
        return farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder);
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
    var mor = eksisterendeMor.isPresent() ? eksisterendeMor.get() : lagreForelder(getForelderDto(fnrMor, null));
    var statusKontrollereFar = StatusKontrollereFar.builder().mor(mor).tidspunktSisteFeiledeForsoek(LocalDateTime.now()).antallFeiledeForsoek(1)
        .build();
    return kontrollereFarDao.save(statusKontrollereFar);
  }

  public Optional<StatusKontrollereFar> henteStatusKontrollereFar(String fnrMor) {
    var statusKontrollereFar = kontrollereFarDao.henteStatusKontrollereFar(fnrMor);
    return statusKontrollereFar;
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
        .henteFarskapserklaeringer(dto.getMor().getFoedselsnummer(), dto.getFar().getFoedselsnummer(), nedreGrense, oevreGrense);

    return respons.isEmpty() ? Optional.empty() : respons.stream().findFirst();
  }

  private ForelderDto getForelderDto(String fnr, Forelderrolle rolle) {
    var navn = personopplysningService.henteNavn(fnr);
    return ForelderDto.builder().forelderrolle(rolle).foedselsnummer(fnr).fornavn(navn.getFornavn()).mellomnavn(navn.getMellomnavn())
        .etternavn(navn.getEtternavn()).build();
  }
}
