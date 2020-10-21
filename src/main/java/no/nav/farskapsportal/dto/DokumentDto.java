package no.nav.farskapsportal.dto;

import java.net.URI;
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
  private String dokumentnavn;
  private byte[] innhold;
  private URI dokumentStatusUrl;
  private URI padesUrl;
  private RedirectUrlDto dokumentRedirectMor;
  private RedirectUrlDto dokumentRedirectFar;
  private LocalDateTime signertAvMor;
  private LocalDateTime signertAvFar;
}
