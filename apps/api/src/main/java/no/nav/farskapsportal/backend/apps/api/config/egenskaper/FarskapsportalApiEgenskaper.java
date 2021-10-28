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
@ConfigurationProperties(prefix = "farskapsportal.egenskaper")
public class FarskapsportalApiEgenskaper {

  private String systembrukerBrukernavn;
  private String systembrukerPassord;
  private boolean brukernotifikasjonerPaa;
  private int minAntallUkerTilTermindato;
  private int maksAntallUkerTilTermindato;
  private int maksAntallMaanederEtterFoedsel;
  private int kontrollFarMaksAntallForsoek;
  private int kontrollFarForsoekFornyesEtterAntallDager;
  private String navOrgnummer;
  private String url;
  private Esignering esignering;

}
