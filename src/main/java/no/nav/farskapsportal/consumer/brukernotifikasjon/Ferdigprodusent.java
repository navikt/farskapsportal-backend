package no.nav.farskapsportal.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.config.BrukernotifikasjonConfig.GRUPPERINGSID_FARSKAP;

import java.time.LocalDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.builders.DoneBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

@Slf4j
@Value
public class Ferdigprodusent {

  String topic;
  String systembruker;
  Koestyrer koeKontroller;

  public void ferdigstilleFarsSigneringsoppgave(String idFarskapserklaering, String foedselsnummerFar) {

    var nokkel = new NokkelBuilder().withEventId(idFarskapserklaering).withSystembruker(systembruker).build();
    var done = oppretteDone(foedselsnummerFar);
    var ferdigmelding = new ProducerRecord<>(topic, nokkel, done);

    koeKontroller.leggeMeldingPaaKoe(ferdigmelding);
  }

  private SpecificRecord oppretteDone(String foedselsnummerFar) {
    return new DoneBuilder()
        .withTidspunkt(LocalDateTime.now())
        .withFodselsnummer(foedselsnummerFar)
        .withGrupperingsId(GRUPPERINGSID_FARSKAP)
        .build();
  }

}
