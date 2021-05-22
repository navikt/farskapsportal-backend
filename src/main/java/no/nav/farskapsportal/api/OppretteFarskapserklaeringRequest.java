package no.nav.farskapsportal.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.nav.farskapsportal.dto.BarnDto;
import org.springframework.validation.annotation.Validated;

@Schema
@Value
@Builder
@Validated
public class OppretteFarskapserklaeringRequest {

  @Parameter(description = "Barnet det skal erklæres farskap for")
  @NonNull BarnDto barn;

  @Parameter(description = "Opplysninger om far, er tom dersom far skal signere erklæring")
  KontrollerePersonopplysningerRequest opplysningerOmFar;

  @Parameter(description = "Mor opplyser om hun bor sammen med far eller ikke", example = "true")
  boolean morBorSammenMedFar;
}
