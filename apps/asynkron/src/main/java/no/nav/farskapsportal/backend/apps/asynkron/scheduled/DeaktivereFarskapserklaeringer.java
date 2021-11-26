package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
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
      var farskapserklaeringerMedUtgaatteSigneringsoppdrag = persistenceService.henteAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag();
    log.info("Fant {} ikke-ferdigstilte farskapserklæringer med signeringsoppdrag eldre enn 40 dager. Deaktiverer disse.",
        farskapserklaeringerMedUtgaatteSigneringsoppdrag.size());
    farskapserklaeringerMedUtgaatteSigneringsoppdrag.forEach(
        farskapserklaering -> persistenceService.deaktivereFarskapserklaering(farskapserklaering.getId()));
    log.info("Sletter dokumentinnhold i deaktiverte farskapserklæringer...");
    for (Farskapserklaering farskapserklaering: farskapserklaeringerMedUtgaatteSigneringsoppdrag) {
      persistenceService.sletteDokumentinnhold(farskapserklaering.getId());
      brukernotifikasjonConsumer.varsleMorOmUtgaattOppgaveForSignering(farskapserklaering.getMor());
    }
    farskapserklaeringerMedUtgaatteSigneringsoppdrag.forEach(
        farskapserklaering -> persistenceService.sletteDokumentinnhold(farskapserklaering.getId()));
  }
}
