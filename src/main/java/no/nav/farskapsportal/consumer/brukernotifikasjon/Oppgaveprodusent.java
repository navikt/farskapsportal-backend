package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.LocalDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Oppgaveprodusent {

  FarskapsportalEgenskaper farskapsportalEgenskaper;
  URL farskapsportalUrl;
  KafkaTemplate kafkaTemplate;

  public void oppretteOppgaveForSigneringAvFarskapserklaering(String idFarskapserklaering, String foedselsnummerFar, String oppgavetekst,
      boolean medEksternVarsling) {

    var nokkel = new NokkelBuilder().withEventId(idFarskapserklaering).withSystembruker(farskapsportalEgenskaper.getSystembrukerBrukernavn()).build();
    var melding = oppretteOppgave(foedselsnummerFar, oppgavetekst, medEksternVarsling);

    try {
      kafkaTemplate.send(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicOppgave(), nokkel, melding);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Oppgave oppretteOppgave(String foedselsnummer, String oppgavetekst, boolean medEksternVarsling) {

    return new OppgaveBuilder()
        .withTidspunkt(LocalDateTime.now())
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withLink(getFarskapsportalUrl())
        .withSikkerhetsnivaa(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave())
        .withTekst(oppgavetekst).build();
  }
}
