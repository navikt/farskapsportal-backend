package no.nav.farskapsportal.backend.apps.api.scheduled.brukernotifikasjon;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.Oppgavebestilling;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Builder
@Slf4j
public class Brukernotifikasjonstyring {

  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;
  private FarskapserklaeringDao farskapserklaeringDao;

  @Scheduled(cron = "${farskapsportal.asynkron.egenskaper.brukernotifikasjon.oppgavestyringsintervall}")
  public void rydddeISigneringsoppgaver() {
    var farskapserklaeringerMedAktiveOppgaver = persistenceService.henteIdTilFarskapserklaeringerMedAktiveOppgaver();

    log.info("Fant {} farskapserklæringer med aktive signeringsoppgaver.", farskapserklaeringerMedAktiveOppgaver.size());

    for (int farskapserklaeringsId : farskapserklaeringerMedAktiveOppgaver) {
      var farskapserklaering = farskapserklaeringDao.findById(farskapserklaeringsId);

      // Sletter oppgaver relatert til ferdigstilte eller deaktiverte erklæringer
      if (farskapserklaering.isPresent()
          && (farskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() != null
          || farskapserklaering.get().getDeaktivert() != null)) {

        var aktiveOppgaver = persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(farskapserklaeringsId,
            farskapserklaering.get().getFar());

        log.info("Fant {} aktive signeringsoppgaver knyttet til ferdigstilt/ deaktivert farskapserklæring med id {}.", aktiveOppgaver.size(),
            farskapserklaeringsId);

        for (Oppgavebestilling oppgave : aktiveOppgaver) {
          log.info("Sletter utdatert signeringsoppgave for far (id {}) i farskapserklæring (id {})", farskapserklaering.get().getFar().getId(),
              farskapserklaeringsId);

          brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(oppgave.getEventId(), farskapserklaering.get().getFar());
        }
      }
    }
  }
}
