package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Builder
public class OppdatereSigneringsstatus {

  private PersistenceService persistenceService;
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Scheduled(cron = "${farskapsportal.asynkron.egenskaper.deaktiveringsrate}", zone = "Europe/Oslo")
  public void oppdatereSigneringsstatus() {

    var signertAvMorFoer = LocalDateTime.now().minusHours(farskapsportalAsynkronEgenskaper.getOppdatereSigneringsstatusMinAntallTimerEtterMorSignering());
    var farskapserklaeringer = persistenceService.henteAktiveFarskapserklaeringerManglerSigneringsinfoFar(signertAvMorFoer);



  }

}
