package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Beskjedprodusent.java
=======
import java.time.ZonedDateTime;
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Beskjedprodusent.java
import java.util.UUID;
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
    var unikEventid = UUID.randomUUID().toString();
<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Beskjedprodusent.java
    return new NokkelBuilder().withSystembruker(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()).withEventId(unikEventid).build();
=======
    return new NokkelBuilder().withSystembruker(farskapsportalEgenskaper.getSystembrukerBrukernavn()).withEventId(unikEventid).build();
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Beskjedprodusent.java
  }

  private Beskjed oppretteBeskjed(String foedselsnummer, String meldingTilBruker, boolean medEksternVarsling, URL lenke) {

    return new BeskjedBuilder()
        .withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withSynligFremTil(
<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Beskjedprodusent.java
            LocalDateTime.now(ZoneId.of("UTC")).withHour(0)
                .plusMonths(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
        .withSikkerhetsnivaa(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed())
=======
            LocalDateTime.now(ZoneId.of("UTC")).withHour(0).plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
        .withSikkerhetsnivaa(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed())
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Beskjedprodusent.java
        .withLink(lenke)
        .withTekst(meldingTilBruker)
        .build();
  }
}
