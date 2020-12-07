package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.farskapsportal.persistence.entity.RedirectUrl;

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
  @ApiModelProperty("Url for å be om status på signeringsoppdrag")
  private URI dokumentStatusUrl;
  @ApiModelProperty("Url for å hente kopi av signert dokument")
  private URI padesUrl;
  @ApiModelProperty("Mors Url for å utføre dokumentsignering hos Posten")
  private RedirectUrlDto redirectUrlMor;
  @ApiModelProperty("Fars Url for å utføre dokumentsignering hos Posten")
  private RedirectUrlDto redirectUrlFar;
  @ApiModelProperty("Signeringstidspunkt mor")
  private LocalDateTime signertAvMor;
  @ApiModelProperty("Signeringstidspunkt far")
  private LocalDateTime signertAvFar;
}
