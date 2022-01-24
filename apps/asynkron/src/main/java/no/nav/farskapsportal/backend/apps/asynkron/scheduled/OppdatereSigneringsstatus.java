package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.api.FarskapsportalApiConsumer;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Builder
public class OppdatereSigneringsstatus {

  private PersistenceService persistenceService;
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;
  private FarskapsportalApiConsumer farskapsportalApiConsumer;

  @Scheduled(cron = "@hourly", zone = "Europe/Oslo")
  public void oppdatereSigneringsstatus() {

    var farSendtTilSigneringFoer = LocalDateTime.now()
        .minusHours(farskapsportalAsynkronEgenskaper.getOppdatereSigneringsstatusMinAntallTimerEtterFarBleSendtTilSignering());
    var ider = persistenceService.henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoFar(farSendtTilSigneringFoer);

    var farskapserklaering_tekst = ider.size() == 1 ? "farskapserklæring" : "farskapserklæringer";

    log.info("Fant id til {} {} som signeringsstatus skal synkroniseres for.", ider.size(), farskapserklaering_tekst);

    for (int id : ider) {
      farskapsportalApiConsumer.synkronisereSigneringsstatus(id);
    }
  }
}
