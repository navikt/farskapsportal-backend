package no.nav.farskapsportal.backend.libs.felles.config.egenskaper;

import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.yaml.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:felles-application.yml", factory = YamlPropertySourceFactory.class)
public class FarskapsportalFellesSystemegenskaper {

}
