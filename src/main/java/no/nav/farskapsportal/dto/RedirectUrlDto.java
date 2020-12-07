package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
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
public class RedirectUrlDto {
  @ApiModelProperty("Signerers URL for signering av dokument")
  private URI redirectUrl;
  @ApiModelProperty("Signerer (mor eller far)")
  private ForelderDto signerer;
}
