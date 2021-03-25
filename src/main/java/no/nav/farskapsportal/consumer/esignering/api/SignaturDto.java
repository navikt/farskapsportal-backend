package no.nav.farskapsportal.consumer.esignering.api;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.farskapsportal.api.StatusSignering;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignaturDto {
  @ApiModelProperty("Signaturens eier")
  String signatureier;

  @ApiModelProperty("Signering er gjennomført")
  boolean harSignert;

  @ApiModelProperty("Tidspunkt for siste statusendring")
  LocalDateTime tidspunktForStatus;

  @ApiModelProperty("Status signering")
  StatusSignering statusSignering;
}
