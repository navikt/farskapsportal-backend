package no.nav.farskapsportal.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.config.BrukernotifikasjonConfig.GRUPPERINGSID_FARSKAP;

import java.net.URL;
import java.time.LocalDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
@Value
public class Oppgaveprodusent {

  String topic;
  int oppgaveSynligIAntallDager;
  int sikkerhetsnivaa;
  String systembrukerFarskapsportal;
  URL urlFarskapsportal;
  Koestyrer koeKontroller;

  public void oppretteOppgaveForSigneringAvFarskapserklaering(String idFarskapserklaering, String foedselsnummerFar, String oppgavetekst,
      boolean medEksternVarsling) {

    var nokkel = new NokkelBuilder().withEventId(idFarskapserklaering).withSystembruker(systembrukerFarskapsportal).build();
    var oppgave = oppretteOppgave(foedselsnummerFar, oppgavetekst, medEksternVarsling);
    var oppgavemelding = new ProducerRecord<>(topic, nokkel, oppgave);
    koeKontroller.leggeMeldingPaaKoe(oppgavemelding);
  }

  private SpecificRecord oppretteOppgave(String foedselsnummer, String oppgavetekst, boolean medEksternVarsling) {

    return new OppgaveBuilder()
        .withTidspunkt(LocalDateTime.now())
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(GRUPPERINGSID_FARSKAP)
        .withEksternVarsling(medEksternVarsling)
        .withLink(urlFarskapsportal)
        .withSikkerhetsnivaa(sikkerhetsnivaa)
        .withTekst(oppgavetekst).build();
  }
}
