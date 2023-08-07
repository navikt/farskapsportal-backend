package no.nav.farskapsportal.backend.apps.api.scheduled.esignering;

import java.io.IOException;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Builder
public class OppdatereSigneringsstatus {

  private PersistenceService persistenceService;
  private FarskapsportalService farskapsportalService;
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @SchedulerLock(name = "signeringsstatus", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
  @Scheduled(cron = "@hourly", zone = "Europe/Oslo")
  public void oppdatereSigneringsstatus() {

    var farSendtTilSigneringFoer = LocalDateTime.now()
        .minusHours(farskapsportalAsynkronEgenskaper.getOppdatereSigneringsstatusMinAntallTimerEtterFarBleSendtTilSignering());
    var ider = persistenceService.henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoFar(farSendtTilSigneringFoer);

    var farskapserklaeringTekst = ider.size() == 1 ? "farskapserklæring" : "farskapserklæringer";

    log.info("Fant id til {} {} som signeringsstatus skal synkroniseres for.", ider.size(), farskapserklaeringTekst);

    for (int id : ider) {
      farskapsportalService.synkronisereSigneringsstatusFar(id);
    }
  }
}
