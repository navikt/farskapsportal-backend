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
public class OppretteFarskaperklaeringRequest {

  @ApiModelProperty("Barnet det skal erklæres farskap for")
  @NonNull BarnDto barn;

  @ApiModelProperty("Opplysninger om far, er tom dersom far skal signere erklæring")
  KontrollerePersonopplysningerRequest opplysningerOmFar;
}
