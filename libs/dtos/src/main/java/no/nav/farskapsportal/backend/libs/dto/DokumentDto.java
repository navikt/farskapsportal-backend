package no.nav.farskapsportal.backend.libs.dto;

import io.swagger.v3.oas.annotations.Parameter;
import java.net.URI;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DokumentDto {
  @Parameter(description = "Navn på farskapserklæringsdokument", example = "farskapserklaering.pdf")
  private String dokumentnavn;
  @Parameter(description = "Mors Url for å utføre dokumentsignering hos Posten")
  private URI redirectUrlMor;
  @Parameter(description = "Fars Url for å utføre dokumentsignering hos Posten")
  private URI redirectUrlFar;
  @Parameter(description = "Signeringstidspunkt mor")
  private LocalDateTime signertAvMor;
  @Parameter(description = "Signeringstidspunkt far")
  private LocalDateTime signertAvFar;
}
