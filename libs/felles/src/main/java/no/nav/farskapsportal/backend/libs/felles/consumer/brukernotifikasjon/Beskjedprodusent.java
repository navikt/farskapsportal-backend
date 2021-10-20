package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Beskjedprodusent.java
import java.util.UUID;
=======
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Beskjedprodusent.java
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedBuilder;
<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Beskjedprodusent.java
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
=======
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.persistence.entity.Forelder;
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Beskjedprodusent.java
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;
  KafkaTemplate<Nokkel, Beskjed> kafkaTemplate;

  public void oppretteBeskjedTilBruker(Forelder forelder, String meldingTilBruker, boolean medEksternVarsling, URL lenke, Nokkel nokkel) {

    var beskjed = oppretteBeskjed(forelder.getFoedselsnummer(), meldingTilBruker, medEksternVarsling, lenke);

<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Beskjedprodusent.java
    kafkaTemplate.send(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBeskjed(), nokkel, beskjed);
  }

  private Nokkel oppretteNokkel() {
    var unikEventid = UUID.randomUUID().toString();
    return new NokkelBuilder().withSystembruker(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()).withEventId(unikEventid).build();
=======
    try {
      kafkaTemplate.send(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed(), nokkel, beskjed);
    } catch (Exception e) {
      log.error("Opprettelse av beskjed til forelder med id {} feilet!", forelder.getId());
    }

    log.info("Beskjed sendt til forelder med id {}.", forelder.getId());
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Beskjedprodusent.java
  }

  private Beskjed oppretteBeskjed(String foedselsnummer, String meldingTilBruker, boolean medEksternVarsling, URL lenke) {

    return new BeskjedBuilder()
        .withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withSynligFremTil(
            LocalDateTime.now(ZoneId.of("UTC")).withHour(0)
<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Beskjedprodusent.java
                .plusMonths(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
        .withSikkerhetsnivaa(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed())
=======
                .plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
        .withSikkerhetsnivaa(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed())
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Beskjedprodusent.java
        .withLink(lenke)
        .withTekst(meldingTilBruker)
        .build();
  }
}
