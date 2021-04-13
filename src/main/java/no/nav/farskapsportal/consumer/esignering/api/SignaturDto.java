package no.nav.farskapsportal.consumer.esignering.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
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
  private String signatureier;

  @ApiModelProperty("Signering er gjennomført")
  private boolean harSignert;

  @ApiModelProperty("Lenke til XAdES XML for signerer")
  private URI xadeslenke;

  @ApiModelProperty("Tidspunkt for siste statusendring")
  private LocalDateTime tidspunktForStatus;

  @ApiModelProperty("Status signering")
  private StatusSignering statusSignering;
}
