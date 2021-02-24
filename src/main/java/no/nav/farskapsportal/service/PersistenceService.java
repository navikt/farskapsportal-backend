package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.util.Utils.toSingletonOrThrow;

import java.time.LocalDate;
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
import no.nav.farskapsportal.exception.EksisterendeFarskapserklaeringException;
import no.nav.farskapsportal.exception.FeilIDatagrunnlagException;
import no.nav.farskapsportal.exception.PersonHarFeilRolleException;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.DokumentDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.entity.Barn;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.exception.FantIkkeEntititetException;
import no.nav.farskapsportal.util.MappingUtil;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class PersistenceService {

  private final FarskapsportalEgenskaper farskapsportalEgenskaperConfig;

  private final FarskapserklaeringDao farskapserklaeringDao;

  private final BarnDao barnDao;

  private final ForelderDao forelderDao;

  private final DokumentDao dokumentDao;

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

  public Dokument lagreDokument(DokumentDto dto) {
    var dokument = mappingUtil.toEntity(dto);
    return dokumentDao.save(dokument);
  }

  @Transactional
  public Farskapserklaering lagreFarskapserklaering(FarskapserklaeringDto dto) {


    if (eksisterendeFarskapserklaering.isEmpty()) {
      var eksisterendeMor = forelderDao.henteForelderMedFnr(dto.getMor().getFoedselsnummer());
      var eksisterendeFar = forelderDao.henteForelderMedFnr(dto.getFar().getFoedselsnummer());

      var farskapserklaering = Farskapserklaering.builder().mor(eksisterendeMor.orElseGet(() -> mappingUtil.toEntity(dto.getMor())))
          .far(eksisterendeFar.orElseGet(() -> mappingUtil.toEntity(dto.getFar()))).barn(mappingUtil.toEntity(dto.getBarn()))
          .dokument(mappingUtil.toEntity(dto.getDokument())).build();

      return farskapserklaeringDao.save(farskapserklaering);
    } else if (gjelderSammeForeldrepar(dto, eksisterendeFarskapserklaering.get())) {
      if (barnErOppgittMedFoedselsnummer) {
        throw new EksisterendeFarskapserklaeringException(Feilkode.ERKLAERING_EKSISTERER_BARN);
      }
    }
    throw new EksisterendeFarskapserklaeringException(Feilkode.ERKLAERING_EKSISTERER_UFOEDT);
  }

  private boolean gjelderSammeForeldrepar(FarskapserklaeringDto ny, Farskapserklaering eksisterende) {
    return eksisterende.getMor().getFoedselsnummer().equals(ny.getMor().getFoedselsnummer()) && eksisterende.getFar().getFoedselsnummer()
        .equals(ny.getFar().getFoedselsnummer());
  }

  private Optional<Farskapserklaering> henteEksisterendeFarskapserklaeringForUfoedtBarn(FarskapserklaeringDto dto) {

    var nedreGrense = LocalDate.now().plusWeeks(farskapsportalEgenskaperConfig.getMinAntallUkerTilTermindato() - 1);
    var oevreGrense = LocalDate.now().plusWeeks(farskapsportalEgenskaperConfig.getMaksAntallUkerTilTermindato());

    var respons = farskapserklaeringDao
        .henteFarskapserklaeringer(dto.getMor().getFoedselsnummer(), dto.getFar().getFoedselsnummer(), nedreGrense, oevreGrense);

    return respons.isEmpty() ? Optional.empty() : respons.stream().findFirst();
  }

  public Set<FarskapserklaeringDto> henteFarskapserklaeringer(String foedselsnummer) {
    var farskapserklaeringer = farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(foedselsnummer);
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
  }

  public Optional<FarskapserklaeringDto> henteMorsEksisterendeErklaering(String fnrMor) {
    var farskapserklaeringer = farskapserklaeringDao.henteMorsErklaeringer(fnrMor);
    return Optional.of(farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(toSingletonOrThrow(new FeilIDatagrunnlagException(Feilkode.MOR_HAR_FLERE_ERKLAERINGER))));
  }

  public Set<FarskapserklaeringDto> henteFarsErklaeringer(String fnrFar) {
    var farskapserklaeringer = farskapserklaeringDao.henteFarsErklaeringer(fnrFar);
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
  }

  public Optional<FarskapserklaeringDto> henteBarnsEksisterendeErklaering(String fnrBarn) {
    var farskapserklaeringer = farskapserklaeringDao.henteBarnsErklaeringer(fnrBarn);
    return Optional.of(farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(toSingletonOrThrow(new FeilIDatagrunnlagException(Feilkode.BARN_HAR_FLERE_ERLAERINGER))));
  }

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
        throw new PersonHarFeilRolleException(String.format("Foreldrerolle %s er foreløpig ikke støttet av løsningen.", forelderrolle));
    }
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
        throw new PersonHarFeilRolleException(String.format("Foreldrerolle %s er foreløpig ikke støttet av løsningen.", forelderrolle));
    }
  }

  private Set<FarskapserklaeringDto> mapTilDto(Set<Farskapserklaering> farskapserklaeringer) {
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(mappingUtil::toDto).collect(Collectors.toSet());
  }

  private Set<FarskapserklaeringDto> henteFarskapserklaeringVedRedirectMorEllerFar(String fnrForelder) {
    var farskapserklaeringer = farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder);
    if (farskapserklaeringer.stream().filter(Objects::nonNull).count() < 1) {
      return mapTilDto(farskapserklaeringer);
    } else {
      return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
    }
  }

  private void morKanOppretteNyFarskapserklaering(String fnrMor, FarskapserklaeringDto dto) {

    var barnErOppgittMedFoedselsnummer = dto.getBarn().getFoedselsnummer() != null && dto.getBarn().getFoedselsnummer().length() > 10;

    // Hente eventuell eksisterende farskapserklaering for mor
    var morsEksisterendeErklaering =  henteMorsEksisterendeErklaering(fnrMor);

    // Hente eventuell eksisterende farskapserklaering for barn, hvis barn oppgitt med fnr
    var barnsEksisterendeErklaering = barnErOppgittMedFoedselsnummer ? henteBarnsEksisterendeErklaering(dto.getBarn().getFoedselsnummer()) : Optional.empty();

    if (morsEksisterendeErklaering.isPresent()) {
      throw new EksisterendeFarskapserklaeringException(Feilkode.ERKLAERING_EKSISTERER_BARN);
    }
    if (barnsEksisterendeErklaering.isPresent()) {
      throw new EksisterendeFarskapserklaeringException(Feilkode.ERKLAERING_EKSISTERER_BARN);
    }

  }
}
