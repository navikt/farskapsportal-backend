package no.nav.farskapsportal.backend.apps.api.consumer.esignering.api;

import io.swagger.v3.oas.annotations.Parameter;
import java.net.URI;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import no.nav.farskapsportal.backend.apps.api.api.StatusSignering;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignaturDto {

  @Parameter(description = "Signaturens eier")
  private String signatureier;

  @Parameter(description = "Signering er gjennomført")
  private boolean harSignert;

  @Parameter(description = "Lenke til XAdES XML for signerer")
  private URI xadeslenke;

  @Parameter(description = "Tidspunkt for siste statusendring")
  private ZonedDateTime tidspunktForStatus;

  @Parameter(description = "Status signering")
  private StatusSignering statusSignering;
}
