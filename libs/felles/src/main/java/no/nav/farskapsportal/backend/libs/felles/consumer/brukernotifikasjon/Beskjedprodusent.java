package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.tms.varsel.action.Sensitivitet;
import no.nav.tms.varsel.action.Varseltype;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  KafkaTemplate<String, String> kafkaTemplate;
  URL farskapsportalUrlForside;
  URL farskapsportalUrlOversikt;
  FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  public void oppretteBeskjedTilBruker(
      Forelder forelder,
      String meldingTilBruker,
      boolean medEksternVarsling,
      String eventId,
      String fodselsnummer) {
    oppretteBeskjedTilBruker(
        forelder, meldingTilBruker, medEksternVarsling, false, eventId, fodselsnummer);
  }

  public void oppretteBeskjedTilBruker(
      Forelder forelder,
      String meldingTilBruker,
      boolean medEksternVarsling,
      boolean lenkeTilOversikt,
      String eventId,
      String fodselsnummer) {

    var farskapsportalUrl = lenkeTilOversikt ? farskapsportalUrlOversikt : farskapsportalUrlForside;

    var beskjed =
        oppretteBeskjed(
            meldingTilBruker, farskapsportalUrl, eventId, fodselsnummer, medEksternVarsling);

    try {
      kafkaTemplate.send(
          farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon(),
          eventId,
          beskjed);
    } catch (Exception e) {
      log.error("Opprettelse av beskjed {} til forelder feilet!", meldingTilBruker, e);
      SIKKER_LOGG.error(
          "Opprettelse av beskjed {} til forelder med id {} feilet!",
          meldingTilBruker,
          forelder.getId(),
          e);
    }

    var medEllerUten = medEksternVarsling ? "med" : "uten";
    log.info(
        "Beskjed {}, {} ekstern varsling og vareventIdselId {} er sendt til forelder.",
        meldingTilBruker,
        medEllerUten,
        eventId);
    SIKKER_LOGG.info(
        "Beskjed {}, {} ekstern varsling og eventId {} er sendt til forelder med personid {}.",
        meldingTilBruker,
        medEllerUten,
        eventId,
        forelder.getId());
  }

  private String oppretteBeskjed(
      String meldingTilBruker,
      URL farskapsportalUrl,
      String eventId,
      String fodselsnummer,
      Boolean medEksternVarsling) {
    log.info("Oppretter beskjed med eventId {} og melding {}", eventId, meldingTilBruker);

    OpprettVarselBuilder builder =
        OpprettVarselBuilder.newInstance()
            .withType(Varseltype.Beskjed)
            .withVarselId(eventId)
            .withSensitivitet(
                Sensitivitet.valueOf(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()))
            .withIdent(fodselsnummer)
            .withTekst("nb", meldingTilBruker, true)
            .withLink(farskapsportalUrl.toString())
            .withAktivFremTil(
                ZonedDateTime.now(ZoneId.of("UTC"))
                    .withHour(0)
                    .plusMonths(
                        farskapsportalFellesEgenskaper
                            .getBrukernotifikasjon()
                            .getSynlighetBeskjedAntallMaaneder()))
            .withProdusent(
                farskapsportalFellesEgenskaper.getCluster(),
                farskapsportalFellesEgenskaper.getNamespace(),
                farskapsportalFellesEgenskaper.getAppnavn());

    if (medEksternVarsling) {
      builder = builder.withEksternVarsling();
    }

    return builder.build();
  }
}
