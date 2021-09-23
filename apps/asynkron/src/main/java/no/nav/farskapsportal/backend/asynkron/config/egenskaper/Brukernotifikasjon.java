package no.nav.farskapsportal.backend.asynkron.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "brukernotifikasjon")
public class Brukernotifikasjon {

  private String grupperingsidFarskap;
  private boolean skruddPaa;
  private int sikkerhetsnivaaBeskjed;
  private int synlighetBeskjedAntallMaaneder;
  private int synlighetOppgaveAntallDager;
  private String topicBeskjed;
  private String topicFerdig;
}
