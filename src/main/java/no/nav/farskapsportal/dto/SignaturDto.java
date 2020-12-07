package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignaturDto {
  @ApiModelProperty("Signaturens eier")
  String signatureier;

  @ApiModelProperty("Signering er gjennomført")
  boolean harSignert;

  @ApiModelProperty("Tidspunkt for signering")
  LocalDateTime tidspunktForSignering;
}
