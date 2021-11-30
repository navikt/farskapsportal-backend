package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.Oppgavebestilling;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Builder
public class DeaktivereFarskapserklaeringer {

  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;

  @Scheduled(cron = "${farskapsportal.asynkron.egenskaper.deaktiveringsrate}", zone = "Europe/Oslo")
  public void vurdereDeaktivering() {
    deaktivereFarskapserklaeringerMedUtgaatteSigneringsoppdrag();
  }

  private void deaktivereFarskapserklaeringerMedUtgaatteSigneringsoppdrag() {
    var idTilFarskapserklaeringerMedUtgaatteSigneringsoppdrag = persistenceService.henteIdTilAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag();

    log.info("Fant {} ikke-ferdigstilte farskapserklæringer med signeringsoppdrag eldre enn 40 dager. Deaktiverer disse.",
        idTilFarskapserklaeringerMedUtgaatteSigneringsoppdrag.size());

    for (int farskapserklaeringsid : idTilFarskapserklaeringerMedUtgaatteSigneringsoppdrag) {
      var farskapserklaering = persistenceService.henteFarskapserklaeringForId(farskapserklaeringsid);
      var aktiveOppgaver = persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(farskapserklaeringsid,
          farskapserklaering.getFar());
      log.info("Fant {} utløpte signeringsoppgaver knyttet til farskapserklæring med id {}.", aktiveOppgaver.size(), farskapserklaeringsid);
      for (Oppgavebestilling oppgave : aktiveOppgaver) {
        log.info("Sletter utgått signeringsoppgave for far (id {}) i farskapserklæring (id {})", farskapserklaering.getFar().getId(),
            farskapserklaeringsid);
        brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(oppgave.getEventId(), farskapserklaering.getFar());
      }

      log.info("Deaktiverer farskapserklæring med id {} ", farskapserklaeringsid);
      persistenceService.deaktivereFarskapserklaering(farskapserklaeringsid);
      log.info("Sletter dokumentinnhold til farskapserklæring med id {}", farskapserklaeringsid);
      persistenceService.sletteDokumentinnhold(farskapserklaeringsid);
      log.info("Varsler mor om utgått signeringsoppave");
      brukernotifikasjonConsumer.varsleMorOmUtgaattOppgaveForSignering(farskapserklaering.getMor());
    }

    log.info(
        "Farskapserklæringer med utgåtte signeringsoppdrag daktivert, relaterte dokumenter er slettet, mor er varslet om utgått signeringsoppgave");
  }
}
