package no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.yaml.YamlPropertySourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@Import(FarskapsportalFellesEgenskaper.class)
@ConfigurationProperties(prefix = "farskapsportal.asynkron.egenskaper")
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
public class FarskapsportalAsynkronEgenskaper {

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  private int arkiveringsintervall;
  private String deaktiveringsrate;
  private int levetidIkkeFerdigstilteSigneringsoppdragIDager;
  private int levetidOversendteFarskapserklaeringerIDager;
  private int levetidErklaeringerIkkeSignertAvMorIDager;
  private int oppgavestyringsforsinkelse;
  private int oppdatereSigneringsstatusMinAntallTimerEtterFarBleSendtTilSignering;
  private Skatt skatt;
}
