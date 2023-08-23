package no.nav.farskapsportal.backend.libs.felles.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.yaml.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bucket")
@PropertySource(
    value = "classpath:felles-application.yml",
    factory = YamlPropertySourceFactory.class)
public class Bucket {

  private String padesName;
  private String xadesName;
}
