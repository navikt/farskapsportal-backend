package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.Arkiv;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Builder
public class Ryddejobb {

  private Arkiv arkiv;
  private PersistenceService persistenceService;

  @SchedulerLock(name = "slette-dokumenter", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
  @Scheduled(cron = "@daily", zone = "Europe/Oslo")
  public void sletteGamleDokumenter() {
    var dokumentArkivertFoer =
        LocalDateTime.now().minusMonths(arkiv.getLevetidDokumenterIMaaneder());

    var idTilGamleFarskapserklaeringer =
        persistenceService
            .henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(
                dokumentArkivertFoer, dokumentArkivertFoer)
            .toArray();

    log.info(
        "Antall farskapserklæringer med dokumenter som er klar til sletting: {}",
        idTilGamleFarskapserklaeringer.length);
    if (idTilGamleFarskapserklaeringer.length
        > arkiv.getMaksAntallDokumenterSomSlettesPerKjoering()) {
      log.info(
          "Begrenser sletting til {} dokumenter i denne kjøringen.",
          arkiv.getMaksAntallDokumenterSomSlettesPerKjoering());
    }

    var antallDokumenterForKjoering = idTilGamleFarskapserklaeringer.length > arkiv.getMaksAntallDokumenterSomSlettesPerKjoering() ? arkiv.getMaksAntallDokumenterSomSlettesPerKjoering() : idTilGamleFarskapserklaeringer.length;

    for (int i = 0; i < antallDokumenterForKjoering; i++) {
      persistenceService.sletteDokumentinnhold((Integer) idTilGamleFarskapserklaeringer[i]);
    }

    log.info("Dokumentsletting gjennomført uten feil.");

    var resterende =
        persistenceService
            .henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(
                dokumentArkivertFoer, dokumentArkivertFoer)
            .toArray();
    log.info(
        "Resterende farskapserklaeringer med gamle dokumenter etter kjøring: {}",
        resterende.length);
  }
}
