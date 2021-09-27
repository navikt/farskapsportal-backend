package no.nav.farskapsportal.backend.apps.api.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.yaml.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class Skatt {

  private int maksAntallForbindelser;
  private int maksAntallForbindelserPerRute;
  private int maksVentetidLesing;
  private int maksVentetidForbindelse;
}
