package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedBuilder;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.persistence.entity.Forelder;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  FarskapsportalEgenskaper farskapsportalEgenskaper;
  KafkaTemplate<Nokkel, Beskjed> kafkaTemplate;

  public void oppretteBeskjedTilBruker(Forelder forelder, String meldingTilBruker, boolean medEksternVarsling, URL lenke, Nokkel nokkel) {

    var beskjed = oppretteBeskjed(forelder.getFoedselsnummer(), meldingTilBruker, medEksternVarsling, lenke);

    try {
      kafkaTemplate.send(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed(), nokkel, beskjed);
    } catch (Exception e) {
      log.error("Opprettelse av beskjed til forelder med id {} feilet!", forelder.getId());
    }

    log.info("Beskjed sendt til forelder med id {}.", forelder.getId());
  }

  private Beskjed oppretteBeskjed(String foedselsnummer, String meldingTilBruker, boolean medEksternVarsling, URL lenke) {

    return new BeskjedBuilder()
        .withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withSynligFremTil(
            LocalDateTime.now(ZoneId.of("UTC")).withHour(0)
                .plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
        .withSikkerhetsnivaa(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed())
        .withLink(lenke)
        .withTekst(meldingTilBruker)
        .build();
  }
}
