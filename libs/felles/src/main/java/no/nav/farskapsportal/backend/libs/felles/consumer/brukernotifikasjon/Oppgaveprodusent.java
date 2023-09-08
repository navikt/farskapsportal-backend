package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.BrukernotifikasjonConfig.NAMESPACE_FARSKAPSPORTAL;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@AllArgsConstructor
public class Oppgaveprodusent {

  private KafkaTemplate kafkaTemplate;
  private PersistenceService persistenceService;
  private URL farskapsportalUrl;
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  public void oppretteOppgaveForSigneringAvFarskapserklaering(
      int idFarskapserklaering, Forelder far, String oppgavetekst, boolean medEksternVarsling) {

    var nokkel =
        new NokkelInputBuilder()
            .withEventId(UUID.randomUUID().toString())
            .withGrupperingsId(
                farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
            .withFodselsnummer(far.getFoedselsnummer())
            .withAppnavn(farskapsportalFellesEgenskaper.getAppnavn())
            .withNamespace(NAMESPACE_FARSKAPSPORTAL)
            .build();
    var melding = oppretteOppgave(oppgavetekst, medEksternVarsling, farskapsportalUrl);

    var farsAktiveSigneringsoppgaver =
        persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(
            idFarskapserklaering, far);

    if (farsAktiveSigneringsoppgaver.isEmpty()) {
      log.info(
          "Oppretter oppgave om signering til far i farskapserklæring med id {}",
          idFarskapserklaering);
      oppretteOppgave(nokkel, melding);
      log.info("Signeringsppgave opprettet for far med id {}.", far.getId());
      persistenceService.lagreNyOppgavebestilling(idFarskapserklaering, nokkel.getEventId());
    }
  }

  private void oppretteOppgave(NokkelInput nokkel, OppgaveInput melding) {
    try {
      kafkaTemplate.send(
          farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicOppgave(),
          nokkel,
          melding);
    } catch (Exception e) {
      e.printStackTrace();
      throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
    }
  }

  private OppgaveInput oppretteOppgave(
      String oppgavetekst, boolean medEksternVarsling, URL farskapsportalUrl) {

    return new OppgaveInputBuilder()
        .withTidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
        .withEksternVarsling(medEksternVarsling)
        .withLink(farskapsportalUrl)
        .withSikkerhetsnivaa(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave())
        .withSynligFremTil(
            ZonedDateTime.now(ZoneId.of("UTC"))
                .plusDays(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getLevetidOppgaveAntallDager())
                .toLocalDateTime())
        .withTekst(oppgavetekst)
        .build();
  }
}
