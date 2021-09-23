package no.nav.farskapsportal.backend.asynkron.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "farskapsportal.asynkron.egenskaper")
public class FarskapsportalAsynkronEgenskaper {

  private boolean innhenteStatusVedPolling;
  private String systembrukerBrukernavn;
  private String systembrukerPassord;
  private int minAntallUkerTilTermindato;
  private int maksAntallUkerTilTermindato;
  private int maksAntallMaanederEtterFoedsel;
  private int kontrollFarMaksAntallForsoek;
  private int kontrollFarForsoekFornyesEtterAntallDager;
  private String navOrgnummer;
  private String url;
  private Brukernotifikasjon brukernotifikasjon;
  private Skatt skatt;
  private int arkiveringsintervall;
  private boolean arkivereIJoark;
}
