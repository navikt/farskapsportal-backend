package no.nav.farskapsportal.backend.lib.felles.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;
import no.nav.farskapsportal.backend.lib.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.lib.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.lib.felles.exception.InternFeilException;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Builder
public class Oppgaveprodusent {

  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;
  private URL farskapsportalUrl;
  private KafkaTemplate kafkaTemplate;

  public void oppretteOppgaveForSigneringAvFarskapserklaering(String idFarskapserklaering, String foedselsnummerFar, String oppgavetekst,
      boolean medEksternVarsling) {

    var nokkel = new NokkelBuilder().withEventId(idFarskapserklaering).withSystembruker(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn())
        .build();
    var melding = oppretteOppgave(foedselsnummerFar, oppgavetekst, medEksternVarsling);
    try {
      kafkaTemplate.send(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicOppgave(), nokkel, melding);
    } catch (Exception e) {
      log.error("Opprettelse av oppgave feilet!");
      e.printStackTrace();
      throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
    }
  }

  private Oppgave oppretteOppgave(String foedselsnummer, String oppgavetekst, boolean medEksternVarsling) {

    return new OppgaveBuilder()
        .withTidspunkt(LocalDateTime.now())
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withLink(farskapsportalUrl)
        .withSikkerhetsnivaa(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave())
        .withTekst(oppgavetekst).build();
  }
}
