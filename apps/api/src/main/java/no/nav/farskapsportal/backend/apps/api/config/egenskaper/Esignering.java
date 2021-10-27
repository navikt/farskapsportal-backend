package no.nav.farskapsportal.backend.apps.api.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.yaml.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

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
