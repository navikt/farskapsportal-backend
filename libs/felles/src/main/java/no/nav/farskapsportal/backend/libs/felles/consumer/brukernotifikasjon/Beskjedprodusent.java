package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;
  KafkaTemplate<Nokkel, Beskjed> kafkaTemplate;

  public void oppretteBeskjedTilBruker(String brukersFoedselsnummer, String meldingTilBruker, boolean medEksternVarsling, URL lenke) {

    var nokkel = oppretteNokkel();
    var beskjed = oppretteBeskjed(brukersFoedselsnummer, meldingTilBruker, medEksternVarsling, lenke);

    kafkaTemplate.send(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBeskjed(), nokkel, beskjed);

  }

  private Nokkel oppretteNokkel() {
    var unikEventid = (Long) Instant.now().toEpochMilli();
    return new NokkelBuilder().withSystembruker(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()).withEventId(Long.toString(unikEventid))
        .build();
  }

  private Beskjed oppretteBeskjed(String foedselsnummer, String meldingTilBruker, boolean medEksternVarsling, URL lenke) {

    return new BeskjedBuilder()
        .withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withSynligFremTil(
            LocalDateTime.now(ZoneId.of("UTC")).withHour(0)
                .plusMonths(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
        .withSikkerhetsnivaa(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed())
        .withLink(lenke)
        .withTekst(meldingTilBruker)
        .build();
  }
}
