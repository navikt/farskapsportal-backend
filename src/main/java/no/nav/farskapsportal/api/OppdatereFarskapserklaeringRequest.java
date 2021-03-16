package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.validation.annotation.Validated;

@ApiModel
@Value
@Builder
@Validated
public class OppdatereFarskapserklaeringRequest {

  @ApiModelProperty(value = "ID til farskapserklæring som skal oppdateres", example = "1000000")
  int idFarskapserklaering;

  @ApiModelProperty(value = "Angir om foreldrene bor sammen", example = "true")
  boolean borSammen;

}
