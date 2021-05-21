package no.nav.farskapsportal.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.config.BrukernotifikasjonConfig.GRUPPERINGSID_FARSKAP;

import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  String topic;
  int beskjedSynligIAntallMaaneder;
  int sikkerhetsnivaa;
  KafkaTemplate kafkaTemplate;

  public void oppretteBeskjedTilBruker(String brukersFoedselsnummer, String meldingTilBruker, boolean medEksternVarsling) {

    var nokkel = oppretteNokkel();
    var beskjed = oppretteBeskjed(brukersFoedselsnummer, meldingTilBruker, medEksternVarsling);
    var melding = new ProducerRecord<>(topic, nokkel, beskjed);

    kafkaTemplate.send(topic, nokkel, melding);
  }

  private Nokkel oppretteNokkel() {
    var unikEventid = (Long) Instant.now().toEpochMilli();
    return new Nokkel("srvfarskapsportal", unikEventid.toString());
  }

  private Beskjed oppretteBeskjed(String foedselsnummer, String meldingTilBruker, boolean medEksternVarsling) {

    return new BeskjedBuilder()
        .withTidspunkt(LocalDateTime.now())
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(GRUPPERINGSID_FARSKAP)
        .withEksternVarsling(medEksternVarsling)
        .withSynligFremTil(LocalDateTime.now().withHour(0).plusMonths(beskjedSynligIAntallMaaneder))
        .withSikkerhetsnivaa(sikkerhetsnivaa)
        .withTekst(meldingTilBruker)
        .build();
  }
}
