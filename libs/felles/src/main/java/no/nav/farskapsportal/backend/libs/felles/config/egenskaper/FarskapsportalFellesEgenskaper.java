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
@ConfigurationProperties(prefix = "farskapsportal.felles.egenskaper")
@PropertySource(
    value = "classpath:felles-application.yml",
    factory = YamlPropertySourceFactory.class)
public class FarskapsportalFellesEgenskaper {

  private String appnavn;
  private int maksAntallMaanederEtterFoedsel;
  private int kontrollFarMaksAntallForsoek;
  private String url;
  private Brukernotifikasjon brukernotifikasjon;

  public String getCluster() {
    return System.getenv().getOrDefault("NAIS_CLUSTER_NAME", "dev");
  }

  public String getNamespace() {
    return System.getenv().getOrDefault("NAIS_NAMESPACE", "farskapsportal");
  }

  public String getAppnavn() {
    return System.getenv().getOrDefault("NAIS_APP_NAME", appnavn);
  }
}
