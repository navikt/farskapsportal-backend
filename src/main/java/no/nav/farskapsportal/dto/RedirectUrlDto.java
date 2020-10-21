package no.nav.farskapsportal.dto;

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
  private URI redirectUrl;
  private ForelderDto signerer;
}
