package no.nav.farskapsportal.persistence;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import no.nav.farskapsportal.api.ForelderRolle;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.modelmapper.ModelMapper;

@RequiredArgsConstructor
public class PersistenceService {

  private final FarskapserklaeringDao farskapserklaeringDao;

  private final ModelMapper modelMapper;

  public void lagreFarskapserklaering(FarskapserklaeringDto dto) {
    var entity = modelMapper.map(dto, Farskapserklaering.class);
    farskapserklaeringDao.save(entity);
  }

  public Set<FarskapserklaeringDto> henteFarskapserklaeringer(
      String foedselsnummer, ForelderRolle rolle) {
    var farskapserklaeringer =
        rolle.equals(ForelderRolle.FAR)
            ? farskapserklaeringDao.hentFarskapserklaeringerFar(foedselsnummer)
            : farskapserklaeringDao.hentFarskapserklaeringerMor(foedselsnummer);

    return farskapserklaeringer.stream()
        .filter(Objects::nonNull)
        .map(fe -> modelMapper.map(fe, FarskapserklaeringDto.class))
        .collect(Collectors.toSet());
  }

  public Optional<FarskapserklaeringDto> henteFarskapserklaeringForBarn(
      String fnrForelder, ForelderRolle rolleForelder, BarnDto barn) {
    Farskapserklaering farskapserklaering = null;

    if (rolleForelder.equals(ForelderRolle.FAR)) {
      farskapserklaering =
          barn.getFoedselsnummer() != null && barn.getFoedselsnummer().length() > 10
              ? farskapserklaeringDao.hentFarskapserklaeringFar(
                  fnrForelder, barn.getFoedselsnummer())
              : farskapserklaeringDao.hentFarskapserklaeringFar(fnrForelder, barn.getTermindato().);
    } else {
      farskapserklaering =
          barn.getFoedselsnummer() != null && barn.getFoedselsnummer().length() > 10
              ? farskapserklaeringDao.hentFarskapserklaeringMor(
                  fnrForelder, barn.getFoedselsnummer())
              : farskapserklaeringDao.hentFarskapserklaeringMor(fnrForelder, LocalDate.barn.getTermindato());
    }

    return Optional.of(modelMapper.map(farskapserklaering, FarskapserklaeringDto.class));
  }
}
