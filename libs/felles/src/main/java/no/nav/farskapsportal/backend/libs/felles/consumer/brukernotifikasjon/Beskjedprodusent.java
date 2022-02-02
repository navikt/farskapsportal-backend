package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedBuilder;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  KafkaTemplate<Nokkel, Beskjed> kafkaTemplate;
  URL farskapsportalUrlForside;
  URL farskapsportalUrlOversikt;
  FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  public void oppretteBeskjedTilBruker(Forelder forelder, String meldingTilBruker, boolean medEksternVarsling, Nokkel nokkel) {
    oppretteBeskjedTilBruker(forelder,meldingTilBruker, medEksternVarsling, false,nokkel );
  }

  public void oppretteBeskjedTilBruker(Forelder forelder, String meldingTilBruker, boolean medEksternVarsling, boolean lenkeTilOversikt, Nokkel nokkel) {

    var farskapsportalUrl = lenkeTilOversikt ? farskapsportalUrlOversikt : farskapsportalUrlForside;

    var beskjed = oppretteBeskjed(forelder.getFoedselsnummer(), meldingTilBruker, medEksternVarsling, farskapsportalUrl);

    try {
      kafkaTemplate.send(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBeskjed(), nokkel, beskjed);
    } catch (Exception e) {
      log.error("Opprettelse av beskjed til forelder med id {} feilet!", forelder.getId());
    }

    var medEllerUten = medEksternVarsling ? "med" : "uten";
    log.info("Beskjed {} ekstern varsling og eventId {} er sendt til forelder (id {}).", medEllerUten, nokkel.getEventId(), forelder.getId());
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
