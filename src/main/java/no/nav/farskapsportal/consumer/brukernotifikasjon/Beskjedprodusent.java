package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  FarskapsportalEgenskaper farskapsportalEgenskaper;
  KafkaTemplate<Nokkel, Beskjed> kafkaTemplate;

  public void oppretteBeskjedTilBruker(String brukersFoedselsnummer, String meldingTilBruker, boolean medEksternVarsling, URL lenke) {

    var nokkel = oppretteNokkel();
    var beskjed = oppretteBeskjed(brukersFoedselsnummer, meldingTilBruker, medEksternVarsling, lenke);

    kafkaTemplate.send(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed(), nokkel, beskjed);

  }

  private Nokkel oppretteNokkel() {
    var unikEventid = UUID.randomUUID().toString();
    return new NokkelBuilder().withSystembruker(farskapsportalEgenskaper.getSystembrukerBrukernavn()).withEventId(unikEventid).build();
  }

  private Beskjed oppretteBeskjed(String foedselsnummer, String meldingTilBruker, boolean medEksternVarsling, URL lenke) {

    return new BeskjedBuilder()
        .withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withSynligFremTil(
            LocalDateTime.now(ZoneId.of("UTC")).withHour(0).plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
        .withSikkerhetsnivaa(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed())
        .withLink(lenke)
        .withTekst(meldingTilBruker)
        .build();
  }
}
