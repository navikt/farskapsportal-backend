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

  public void ferdigstilleFarsSigneringsoppgave(Forelder far, String varselId) {

    var oppgaveSomSkalFerdigstilles = oppgavebestillingDao.henteOppgavebestilling(varselId);

    if (oppgaveSomSkalFerdigstilles.isPresent()
        && oppgaveSomSkalFerdigstilles.get().getFerdigstilt() == null) {
      var melding = oppretteDone(varselId);
      try {
        kafkaTemplate.send(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon(),
            varselId,
            melding);
      } catch (Exception e) {
        throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
      }

      log.info("Ferdigmelding ble sendt for oppgave med varselId {}.", varselId);
      persistenceService.setteOppgaveTilFerdigstilt(varselId);
    } else {
      log.warn(
          "Fant ingen aktiv oppgavebestilling for varselId {}. Bestiller derfor ikke ferdigstilling.",
          varselId);
      SIKKER_LOGG.warn(
          "Fant ingen aktiv oppgavebestilling for varselId {} (gjelder far med id: {}). Bestiller derfor ikke ferdigstilling.",
          varselId,
          far.getId());
    }
  }

  private String oppretteDone(String varselId) {
    return InaktiverVarselBuilder.newInstance()
        .withVarselId(varselId)
        .withProdusent(
            farskapsportalFellesEgenskaper.getCluster(),
            farskapsportalFellesEgenskaper.getNamespace(),
            farskapsportalFellesEgenskaper.getAppnavn())
        .build();
  }
}
