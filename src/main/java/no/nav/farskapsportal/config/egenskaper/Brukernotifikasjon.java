package no.nav.farskapsportal.config.egenskaper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "brukernotifikasjon")
public class Brukernotifikasjon {

  private int antallDagerForsinkelseEtterMorHarSignert;
  private String topicBeskjed;
  private String topicFerdig;
  private String topicOppgave;
  private String grupperingsidFarskap;
  private int synlighetBeskjedAntallMaaneder;
  private int synlighetOppgaveAntallDager;
  private int sikkerhetsnivaaBeskjed;
  private int sikkerhetsnivaaOppgave;
  private boolean skruddPaa;

}
