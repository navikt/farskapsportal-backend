package no.nav.farskapsportal.service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.config.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.pdl.api.KjoennTypeDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.FarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException;
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
import org.modelmapper.ModelMapper;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class PersistenceService {

  private final FarskapsportalEgenskaper farskapsportalEgenskaperConfig;

  private final FarskapserklaeringDao farskapserklaeringDao;

  private final BarnDao barnDao;

  private final ForelderDao forelderDao;

  private final DokumentDao dokumentDao;

  private final ModelMapper modelMapper;

  public Barn lagreBarn(BarnDto dto) {
    var entity = modelMapper.map(dto, Barn.class);
    return barnDao.save(entity);
  }

  public BarnDto henteBarn(int id) {
    var barn = barnDao.findById(id).orElseThrow(() -> new FantIkkeEntititetException(String.format("Fant ikke barn med id %d i databasen", id)));
    return modelMapper.map(barn, BarnDto.class);
  }

  public Forelder lagreForelder(ForelderDto forelderDto) {
    var forelder = modelMapper.map(forelderDto, Forelder.class);
    return forelderDao.save(forelder);
  }

  public ForelderDto henteForelder(int id) {
    var forelder = forelderDao.findById(id)
        .orElseThrow(() -> new FantIkkeEntititetException(String.format("Fant ingen forelder med id %d i databasen", id)));
    return modelMapper.map(forelder, ForelderDto.class);
  }

  public Dokument lagreDokument(DokumentDto dto) {
    var dokument = modelMapper.map(dto, Dokument.class);
    return dokumentDao.save(dokument);
  }

  @Transactional
  public Farskapserklaering lagreFarskapserklaering(FarskapserklaeringDto dto) {

    var barnErOppgittMedFoedselsnummer = dto.getBarn().getFoedselsnummer() != null && dto.getBarn().getFoedselsnummer().length() > 10;

    Optional<Farskapserklaering> eksisterendeFarskapserklaering = barnErOppgittMedFoedselsnummer ? farskapserklaeringDao
        .henteUnikFarskapserklaering(dto.getMor().getFoedselsnummer(), dto.getFar().getFoedselsnummer(), dto.getBarn().getFoedselsnummer())
        : henteEksisterendeFarskapserklaeringForUfoedtBarn(dto);

    if (eksisterendeFarskapserklaering.isEmpty()) {
      var eksisterendeMor = forelderDao.henteForelderMedFnr(dto.getMor().getFoedselsnummer());
      var eksisterendeFar = forelderDao.henteForelderMedFnr(dto.getFar().getFoedselsnummer());

      var farskapserklaering = Farskapserklaering.builder().mor(eksisterendeMor.orElseGet(() -> modelMapper.map(dto.getMor(), Forelder.class)))
          .far(eksisterendeFar.orElseGet(() -> modelMapper.map(dto.getFar(), Forelder.class))).barn(modelMapper.map(dto.getBarn(), Barn.class))
          .dokument(modelMapper.map(dto.getDokument(), Dokument.class)).build();

      return farskapserklaeringDao.save(farskapserklaering);
    } else if (gjelderSammeForeldrepar(dto, eksisterendeFarskapserklaering.get())) {
      if (barnErOppgittMedFoedselsnummer) {
        throw new FarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException(Feilkode.ERKLAERING_EKSISTERER);
      }
    }
    throw new FarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException(Feilkode.ERKLAERING_EKSISTERER_UFOEDT);
  }

  private boolean gjelderSammeForeldrepar(FarskapserklaeringDto ny, Farskapserklaering gammel) {
    return gammel.getMor().getFoedselsnummer().equals(ny.getMor().getFoedselsnummer()) && gammel.getFar().getFoedselsnummer()
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

    return farskapserklaeringer.stream().filter(Objects::nonNull).map(fe -> modelMapper.map(fe, FarskapserklaeringDto.class))
        .collect(Collectors.toSet());
  }

  @Transactional(readOnly = true)
  public Set<FarskapserklaeringDto> henteAktiveFarskapserklaeringer(String fnrForelder, Forelderrolle forelderrolle, KjoennTypeDto gjeldendeKjoenn) {
    switch (forelderrolle) {
      case MOR:
        return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder));
      case FAR:
        return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
      case MOR_ELLER_FAR:
        if (KjoennTypeDto.KVINNE.equals(gjeldendeKjoenn)) {
          return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder));
        } else if (KjoennTypeDto.MANN.equals(gjeldendeKjoenn)) {
          return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
        }

      default:
        throw new PersonHarFeilRolleException(String.format("Foreldrerolle %s er foreløpig ikke støttet av løsningen.", forelderrolle));
    }
  }

  @Transactional
  public Set<Farskapserklaering>  henteFarskapserklaeringerEtterRedirect(String fnrForelder, Forelderrolle forelderrolle,
      KjoennTypeDto gjeldendeKjoenn) {
    switch (forelderrolle) {
      case MOR:
        return farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder);
      case FAR:
        return farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder);
      case MOR_ELLER_FAR:
        if (KjoennTypeDto.KVINNE.equals(gjeldendeKjoenn)) {
          return farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder);
        } else if (KjoennTypeDto.MANN.equals(gjeldendeKjoenn)) {
          return farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder);
        }

      default:
        throw new PersonHarFeilRolleException(String.format("Foreldrerolle %s er foreløpig ikke støttet av løsningen.", forelderrolle));
    }
  }

  private Set<FarskapserklaeringDto> mapTilDto(Set<Farskapserklaering> farskapserklaeringer) {
    return farskapserklaeringer.stream().filter(Objects::nonNull).map(fe -> modelMapper.map(fe, FarskapserklaeringDto.class))
        .collect(Collectors.toSet());
  }

  private Set<FarskapserklaeringDto> henteFarskapserklaeringVedRedirectMorEllerFar(String fnrForelder) {
    var farskapserklaeringer = farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder);
    if (farskapserklaeringer.stream().filter(Objects::nonNull).count() < 1) {
      return mapTilDto(farskapserklaeringer);
    } else {
      return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
    }
  }
}
