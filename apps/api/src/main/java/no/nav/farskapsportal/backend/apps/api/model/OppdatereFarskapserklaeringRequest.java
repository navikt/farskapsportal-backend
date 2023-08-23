package no.nav.farskapsportal.backend.apps.api.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Schema
@Value
@Builder
public class OppdatereFarskapserklaeringRequest {

  @Parameter(description = "ID til farskapserklæring som skal oppdateres", example = "1000000")
  int idFarskapserklaering;

  @NotNull
  @Parameter(description = "Angir om far bor sammen med mor", example = "true")
  Boolean farBorSammenMedMor;
}
