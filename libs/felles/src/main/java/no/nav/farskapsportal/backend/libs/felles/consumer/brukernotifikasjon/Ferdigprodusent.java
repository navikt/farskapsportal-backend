package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Ferdigprodusent {

  KafkaTemplate kafkaTemplate;
  PersistenceService persistenceService;
  OppgavebestillingDao oppgavebestillingDao;
  FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  public void ferdigstilleFarsSigneringsoppgave(Forelder far, NokkelInput nokkel) {

    var oppgaveSomSkalFerdigstilles =
        oppgavebestillingDao.henteOppgavebestilling(nokkel.getEventId());

    if (oppgaveSomSkalFerdigstilles.isPresent()
        && oppgaveSomSkalFerdigstilles.get().getFerdigstilt() == null) {
      var melding = oppretteDone();
      try {
        kafkaTemplate.send(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicFerdig(),
            nokkel,
            melding);
      } catch (Exception e) {
        throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
      }

      log.info("Ferdigmelding ble sendt for oppgave med eventId {}.");
      persistenceService.setteOppgaveTilFerdigstilt(nokkel.getEventId());
    } else {
      log.warn(
          "Fant ingen aktiv oppgavebestilling for eventId {} (gjelder far med id: {}). Bestiller derfor ikke ferdigstilling.",
          nokkel.getEventId(),
          far.getId());
    }
  }

  private DoneInput oppretteDone() {
    return new DoneInputBuilder()
        .withTidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
        .build();
  }
}
