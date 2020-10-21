package no.nav.farskapsportal.service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.RedirectUrlDto;
import no.nav.farskapsportal.exception.PersonHarFeilRolleException;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.DokumentDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.RedirectUrlDao;
import no.nav.farskapsportal.persistence.entity.Barn;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.RedirectUrl;
import org.modelmapper.ModelMapper;

@RequiredArgsConstructor
public class PersistenceService {

  private final FarskapserklaeringDao farskapserklaeringDao;

  private final BarnDao barnDao;

  private final ForelderDao forelderDao;

  private final RedirectUrlDao redirectUrlDao;

  private final DokumentDao dokumentDao;

  private final ModelMapper modelMapper;

  public void lagreBarn(BarnDto dto) {
    var entity = modelMapper.map(dto, Barn.class);
    barnDao.save(entity);
  }

  public  BarnDto henteBarn(int id) {
    var barn = barnDao.findById(id).get();
    return modelMapper.map(barn, BarnDto.class);
  }

  public void lagreForelder(ForelderDto forelderDto){
    var forelder = modelMapper.map(forelderDto, Forelder.class);
    forelderDao.save(forelder);
  }

  public ForelderDto henteForelder(int id) {
    var forelder = forelderDao.findById(1).get();
    return modelMapper.map(forelder, ForelderDto.class);
  }

  public void lagreRedirectUrl(RedirectUrlDto redirectUrlDto) {
    var redirectUrl = modelMapper.map(redirectUrlDto, RedirectUrl.class);
    redirectUrlDao.save(redirectUrl);
  }

  public RedirectUrlDto henteRedirectUrl(int id) {
    var redirectUrl = redirectUrlDao.findById(id).get();
    return modelMapper.map(redirectUrl, RedirectUrlDto.class);
  }

  public void lagreDokument(DokumentDto dto) {
    var dokument = modelMapper.map(dto, Dokument.class);
    dokumentDao.save(dokument);
  }

  public DokumentDto henteDokument(int id) {
    var dokument = dokumentDao.findById(id).get();
    return modelMapper.map(dokument, DokumentDto.class);
  }

  public void lagreFarskapserklaering(FarskapserklaeringDto dto) {
    var entity = modelMapper.map(dto, Farskapserklaering.class);
    farskapserklaeringDao.save(entity);
  }

  public Set<FarskapserklaeringDto> henteFarskapserklaeringer(String foedselsnummer) {
    var farskapserklaeringer =
        farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(foedselsnummer);

    return farskapserklaeringer.stream()
        .filter(Objects::nonNull)
        .map(fe -> modelMapper.map(fe, FarskapserklaeringDto.class))
        .collect(Collectors.toSet());
  }

  public Set<FarskapserklaeringDto> henteFarskapserklaeringerSomManglerMorsSignatur(
      String fnrForelder, Forelderrolle forelderrolle) {
    switch (forelderrolle) {
      case MOR:
        return mapTilDto(
            farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder));
      case FAR:
        return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
      case MOR_ELLER_FAR:
        return henteFarskapserklaeringVedRedirectMorEllerFar(fnrForelder);
      default:
        throw new PersonHarFeilRolleException(
            String.format(
                "Foreldrerolle %s er foreløpig ikke støttet av løsningen.", forelderrolle));
    }
  }

  private Set<FarskapserklaeringDto> mapTilDto(Set<Farskapserklaering> farskapserklaeringer) {
    return farskapserklaeringer.stream()
        .filter(Objects::nonNull)
        .map(fe -> modelMapper.map(fe, FarskapserklaeringDto.class))
        .collect(Collectors.toSet());
  }

  private Set<FarskapserklaeringDto> henteFarskapserklaeringVedRedirectMorEllerFar(
      String fnrForelder) {
    var farskapserklaeringer =
        farskapserklaeringDao.hentFarskapserklaeringerMorUtenPadeslenke(fnrForelder);
    if (farskapserklaeringer.stream().filter(Objects::nonNull).count() < 1) {
      return mapTilDto(farskapserklaeringer);
    } else {
      return mapTilDto(farskapserklaeringDao.hentFarskapserklaeringerMedPadeslenke(fnrForelder));
    }
  }
}
