package no.nav.farskapsportal.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import org.springframework.validation.annotation.Validated;

@Schema
@Value
@Builder
@Validated
public class OppdatereFarskapserklaeringRequest {

  @Parameter(description = "ID til farskapserklæring som skal oppdateres", example = "1000000")
  int idFarskapserklaering;

  @Parameter(description = "Angir om foreldrene bor sammen", example = "true")
  boolean borSammen;

}
