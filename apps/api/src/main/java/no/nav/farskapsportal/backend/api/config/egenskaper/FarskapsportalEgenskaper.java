package no.nav.farskapsportal.backend.api.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.lib.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "farskapsportal.egenskaper")
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
