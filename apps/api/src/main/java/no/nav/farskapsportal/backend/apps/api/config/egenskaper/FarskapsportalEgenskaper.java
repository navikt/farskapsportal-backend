package no.nav.farskapsportal.backend.apps.api.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.yaml.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "farskapsportal.egenskaper")
@PropertySource(value = {"classpath:application.yml"}, factory = YamlPropertySourceFactory.class)
public class FarskapsportalEgenskaper extends FarskapsportalFellesEgenskaper {

  private boolean innhenteStatusVedPolling;
  private String systembrukerBrukernavn;
  private String systembrukerPassord;
  private int minAntallUkerTilTermindato;
  private int maksAntallUkerTilTermindato;
  private int maksAntallMaanederEtterFoedsel;
  private int kontrollFarForsoekFornyesEtterAntallDager;
  private String navOrgnummer;
  private String url;
  private Esignering esignering;
  private Skatt skatt;
  private int arkiveringsintervall;
  private boolean arkivereIJoark;
}
