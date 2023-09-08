package no.nav.farskapsportal.backend.apps.api.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Getter
@Setter
@Import(FarskapsportalFellesEgenskaper.class)
@Configuration
@ConfigurationProperties(prefix = "farskapsportal.egenskaper")
public class FarskapsportalApiEgenskaper {

  @Autowired private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  private boolean brukernotifikasjonerPaa;
  private int minAntallUkerTilTermindato;
  private int maksAntallUkerTilTermindato;
  private int kontrollFarForsoekFornyesEtterAntallDager;
  private String navOrgnummer;
  private Esignering esignering;
}
