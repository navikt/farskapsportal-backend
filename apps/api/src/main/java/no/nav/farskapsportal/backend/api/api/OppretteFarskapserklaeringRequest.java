package no.nav.farskapsportal.backend.api.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.lib.dto.BarnDto;
import org.springframework.validation.annotation.Validated;

@Schema
@Value
@Builder
@Validated
public class OppretteFarskapserklaeringRequest {

  @NotNull
  @Parameter(description = "Barnet det skal erklæres farskap for")
  BarnDto barn;

  @Valid
  @NotNull
  @Parameter(description = "Opplysninger om far, er tom dersom far skal signere erklæring")
  KontrollerePersonopplysningerRequest opplysningerOmFar;

  @NotNull
  @Parameter(description = "Skriftspråket erklæringen skal være på")
  Skriftspraak skriftspraak;

}
