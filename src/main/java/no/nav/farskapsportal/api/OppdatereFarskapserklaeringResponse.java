package no.nav.farskapsportal.api;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import org.springframework.validation.annotation.Validated;

@ApiModel
@Value
@Builder
@Validated
public class OppdatereFarskapserklaeringResponse {

  @ApiModelProperty(value = "Oppdatert farskapserklæring")
  FarskapserklaeringDto oppdatertFarskapserklaeringDto;

}
