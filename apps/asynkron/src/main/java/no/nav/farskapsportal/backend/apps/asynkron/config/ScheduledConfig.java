package no.nav.farskapsportal.backend.apps.asynkron.config;

import static no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SCHEDULED_TEST;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;

import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.api.FarskapsportalApiConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.scheduled.DeaktivereFarskapserklaeringer;
import no.nav.farskapsportal.backend.apps.asynkron.scheduled.OppdatereSigneringsstatus;
import no.nav.farskapsportal.backend.apps.asynkron.scheduled.Oppgavestyring;
import no.nav.farskapsportal.backend.apps.asynkron.scheduled.Varsel;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Profile({PROFILE_LIVE, PROFILE_SCHEDULED_TEST})
@Configuration
@EnableScheduling
@ComponentScan
public class ScheduledConfig {

  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  public ScheduledConfig(@Autowired FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper) {
    this.farskapsportalAsynkronEgenskaper = farskapsportalAsynkronEgenskaper;
  }

  @Bean
  public DeaktivereFarskapserklaeringer deaktivereFarskapserklaeringer(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      PersistenceService persistenceService) {
    return DeaktivereFarskapserklaeringer.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .persistenceService(persistenceService).build();
  }

  @Bean
  public OppdatereSigneringsstatus oppdatereSigneringsstatus(PersistenceService persistenceService,
      FarskapsportalApiConsumer farskapsportalApiConsumer) {

    return OppdatereSigneringsstatus.builder()
        .farskapsportalApiConsumer(farskapsportalApiConsumer)
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .persistenceService(persistenceService).build();
  }

  @Bean
  public Oppgavestyring oppgavestyring(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      FarskapserklaeringDao farskapserklaeringDao,
      PersistenceService persistenceService) {

    return Oppgavestyring.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .farskapserklaeringDao(farskapserklaeringDao)
        .persistenceService(persistenceService)
        .build();
  }

  @Bean
  public Varsel varsel(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      PersistenceService persistenceService
  ) {

    return Varsel.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .persistenceService(persistenceService)
        .build();
  }
}
