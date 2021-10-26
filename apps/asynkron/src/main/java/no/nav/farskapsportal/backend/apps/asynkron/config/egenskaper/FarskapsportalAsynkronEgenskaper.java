package no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.yaml.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "farskapsportal.asynkron.egenskaper")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class FarskapsportalAsynkronEgenskaper {

  private int arkiveringsintervall;
  private boolean arkivereIJoark;
  private int brukernotifikasjonOppgaveSynlighetAntallDager;
  private int oppgaveslettingsintervall;
  private String systembrukerBrukernavn;
  private String systembrukerPassord;
  private String urlFarskapsportal;
  private Skatt skatt;
}
