package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.tms.varsel.builder.InaktiverVarselBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Ferdigprodusent {

  KafkaTemplate kafkaTemplate;
  PersistenceService persistenceService;
  OppgavebestillingDao oppgavebestillingDao;
  FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  public void ferdigstilleFarsSigneringsoppgave(Forelder far, String eventId) {

    var oppgaveSomSkalFerdigstilles = oppgavebestillingDao.henteOppgavebestilling(eventId);

    if (oppgaveSomSkalFerdigstilles.isPresent()
        && oppgaveSomSkalFerdigstilles.get().getFerdigstilt() == null) {
      var melding = oppretteDone(eventId);
      try {
        kafkaTemplate.send(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon(),
            eventId,
            melding);
      } catch (Exception e) {
        throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
      }

      log.info("Ferdigmelding ble sendt for oppgave med eventId {}.", eventId);
      persistenceService.setteOppgaveTilFerdigstilt(eventId);
    } else {
      log.warn(
          "Fant ingen aktiv oppgavebestilling for eventId {}. Bestiller derfor ikke ferdigstilling.",
          eventId);
      SIKKER_LOGG.warn(
          "Fant ingen aktiv oppgavebestilling for eventId {} (gjelder far med id: {}). Bestiller derfor ikke ferdigstilling.",
          eventId,
          far.getId());
    }
  }

  private String oppretteDone(String eventId) {
    log.info("Inaktiverer varsel med eventId {}", eventId);

    return InaktiverVarselBuilder.newInstance()
        .withVarselId(eventId)
        .withProdusent(
            farskapsportalFellesEgenskaper.getCluster(),
            farskapsportalFellesEgenskaper.getNamespace(),
            farskapsportalFellesEgenskaper.getAppnavn())
        .build();
  }
}
