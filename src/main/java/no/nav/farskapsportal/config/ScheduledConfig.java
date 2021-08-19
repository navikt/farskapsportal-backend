package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;

import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.scheduled.ArkivereFarskapserklaeringer;
import no.nav.farskapsportal.scheduled.SletteOppgave;
import no.nav.farskapsportal.service.PersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Profile(PROFILE_LIVE)
@Configuration
@EnableScheduling
@ComponentScan
public class ScheduledConfig {

  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  public ScheduledConfig(@Autowired FarskapsportalEgenskaper farskapsportalEgenskaper) {
    this.farskapsportalEgenskaper = farskapsportalEgenskaper;
  }

  @Bean
  public ArkivereFarskapserklaeringer arkivereFarskapserklaeringer(PersistenceService persistenceService, SkattConsumer skattConsumer) {
    return ArkivereFarskapserklaeringer.builder()
        .intervallMellomForsoek(farskapsportalEgenskaper.getSkatt().getIntervallOverfoering())
        .persistenceService(persistenceService)
        .skattConsumer(skattConsumer)
        .build();
  }

  @Bean
  public SletteOppgave sletteOppgave(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      PersistenceService persistenceService) {
    return SletteOppgave.builder()
        .farskapsportalEgenskaper(farskapsportalEgenskaper)
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .persistenceService(persistenceService)
        .build();
  }
}
