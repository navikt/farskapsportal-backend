package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@ApiModel
@Value
@Builder
public class KontrollerePersonopplysningerRequest {
  @ApiModelProperty(value = "Fødselsnummer til personen som sjekkes", position = 1)
  String foedselsnummer;

  @ApiModelProperty(value = "Personens fornavn, mellomnavn (hvis aktuelt), og etternavn", position = 2)
  String navn;

}
