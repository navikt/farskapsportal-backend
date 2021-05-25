package no.nav.farskapsportal.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import org.springframework.validation.annotation.Validated;

@Schema
@Value
@Builder
@Validated
public class HenteFarskapserklaeringRequest {

  BarnDto barn;
  ForelderDto motpart;

}
