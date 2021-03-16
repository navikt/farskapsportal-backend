package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import org.springframework.validation.annotation.Validated;

@ApiModel
@Value
@Builder
@Validated
public class OppretteFarskapserklaeringRequest {

  @ApiModelProperty("Barnet det skal erklæres farskap for")
  @NonNull BarnDto barn;

  @ApiModelProperty("Opplysninger om far, er tom dersom far skal signere erklæring")
  KontrollerePersonopplysningerRequest opplysningerOmFar;

  @ApiModelProperty(value = "Mor opplyser om hun bor sammen med far eller ikke", example = "true")
  @NonNull boolean morBorSammenMedFar;
}
