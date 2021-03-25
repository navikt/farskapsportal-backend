package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
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
  @ApiModelProperty(value = "Navn på farskapserklæringsdokument", example = "farskapserklaering.pdf")
  private String dokumentnavn;
  @ApiModelProperty("Dokumentinnhold")
  private byte[] innhold;
  @ApiModelProperty("Mors Url for å utføre dokumentsignering hos Posten")
  private URI redirectUrlMor;
  @ApiModelProperty("Fars Url for å utføre dokumentsignering hos Posten")
  private URI redirectUrlFar;
  @ApiModelProperty("Signeringstidspunkt mor")
  private LocalDateTime signertAvMor;
  @ApiModelProperty("Signeringstidspunkt far")
  private LocalDateTime signertAvFar;
}
