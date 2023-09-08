package no.nav.farskapsportal.backend.apps.api.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "esignering")
public class Esignering {

  private boolean innhenteStatusVedPolling;
  private String suksessUrl;
  private String avbruttUrl;
  private String feiletUrl;
}
