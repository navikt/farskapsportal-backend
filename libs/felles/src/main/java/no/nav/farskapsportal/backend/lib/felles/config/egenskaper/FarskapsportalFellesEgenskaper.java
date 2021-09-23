package no.nav.farskapsportal.backend.lib.felles.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "farskapsportal.felles.egenskaper")
public class FarskapsportalFellesEgenskaper {

  private String systembrukerBrukernavn;
  private String systembrukerPassord;
  private int maksAntallMaanederEtterFoedsel;
  private int kontrollFarMaksAntallForsoek;
  private String url;
  private Brukernotifikasjon brukernotifikasjon;
}
