package no.nav.farskapsportal.backend.apps.asynkron.config;

import static no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SCHEDULED_TEST;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;

import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.joark.JournalpostApiConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.scheduled.ArkivereFarskapserklaeringer;
import no.nav.farskapsportal.backend.apps.asynkron.scheduled.DeaktivereFarskapserklaeringer;
import no.nav.farskapsportal.backend.apps.asynkron.scheduled.Oppgavestyring;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
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
  public ArkivereFarskapserklaeringer arkivereFarskapserklaeringer(
      JournalpostApiConsumer journalpostApiConsumer,
      PersistenceService persistenceService,
      SkattConsumer skattConsumer) {

    return ArkivereFarskapserklaeringer.builder()
        .arkivereIJoark(farskapsportalAsynkronEgenskaper.isArkivereIJoark())
        .intervallMellomForsoek(farskapsportalAsynkronEgenskaper.getArkiveringsintervall())
        .journalpostApiConsumer(journalpostApiConsumer)
        .persistenceService(persistenceService)
        .skattConsumer(skattConsumer)
        .build();
  }

  @Bean
  public DeaktivereFarskapserklaeringer deaktivereFarskapserklaeringer(PersistenceService persistenceService) {
    return DeaktivereFarskapserklaeringer.builder().persistenceService(persistenceService).build();
  }

  @Bean
  public Oppgavestyring sletteOppgave(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      PersistenceService persistenceService) {
    return Oppgavestyring.builder()
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .persistenceService(persistenceService)
        .build();
  }
}
