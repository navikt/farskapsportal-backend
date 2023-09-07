package no.nav.farskapsportal.backend.apps.api.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.dto.FarskapserklaeringDto;
import org.springframework.validation.annotation.Validated;

@Schema
@Value
@Builder
@Validated
public class OppdatereFarskapserklaeringResponse {

  @Parameter(description = "Oppdatert farskapserklæring")
  FarskapserklaeringDto oppdatertFarskapserklaeringDto;
}
