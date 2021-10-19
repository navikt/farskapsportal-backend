package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.DoneBuilder;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.exception.InternFeilException;
import no.nav.farskapsportal.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.service.PersistenceService;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Ferdigprodusent {

  FarskapsportalEgenskaper farskapsportalEgenskaper;
  KafkaTemplate kafkaTemplate;
  PersistenceService persistenceService;
  OppgavebestillingDao oppgavebestillingDao;

  public void ferdigstilleFarsSigneringsoppgave(Forelder far, Nokkel nokkel) {

    var oppgaveSomSkalFerdigstilles = oppgavebestillingDao.henteOppgavebestilling(nokkel.getEventId());

    if (oppgaveSomSkalFerdigstilles.isPresent() && oppgaveSomSkalFerdigstilles.get().getFerdigstilt() == null) {
      var melding = oppretteDone(far.getFoedselsnummer());
      try {
        kafkaTemplate.send(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig(), nokkel, melding);
      } catch (Exception e) {
        throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
      }

      log.info("Ferdigmelding ble sendt for oppgave med eventId {}.");
      persistenceService.setteOppgaveTilFerdigstilt(nokkel.getEventId());
    } else {
      log.warn("Fant ingen aktiv oppgavebestilling for eventId {} (gjelder far med id: {}). Bestiller derfor ikke ferdigstilling.",
          nokkel.getEventId(), far.getId());
    }
  }

  private Done oppretteDone(String foedselsnummerFar) {
    return new DoneBuilder()
        .withTidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
        .withFodselsnummer(foedselsnummerFar)
        .withGrupperingsId(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .build();
  }
}
