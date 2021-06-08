package no.nav.farskapsportal.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "esignering")
public class Esignering {

  private boolean innhenteStatusVedPolling;
  private String suksessUrl;
  private String avbruttUrl;
  private String feiletUrl;

}
