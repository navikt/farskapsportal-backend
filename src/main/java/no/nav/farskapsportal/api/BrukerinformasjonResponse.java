package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;

@ApiModel
@Value
@Builder
public class BrukerinformasjonResponse {

  boolean kanOppretteFarskapserklaering;
  boolean gyldigForelderrolle;
  Forelderrolle forelderrolle;
  Set<FarskapserklaeringDto> farsVentendeFarskapserklaeringer;
  Set<FarskapserklaeringDto> morsVentendeFarskapserklaeringer;
  Set<String> fnrNyligFoedteBarnUtenRegistrertFar;
}
