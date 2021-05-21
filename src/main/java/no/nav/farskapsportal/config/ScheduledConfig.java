package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;

import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.scheduled.OverfoereTilSkatt;
import no.nav.farskapsportal.scheduled.SletteOppgave;
import no.nav.farskapsportal.scheduled.VarsleFarOmSigneringsoppgave;
import no.nav.farskapsportal.service.PersistenceService;
import org.springframework.beans.factory.annotation.Value;
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

  @Bean
  public OverfoereTilSkatt overfoereTilSkatt(PersistenceService persistenceService, SkattConsumer skattConsumer) {
    return OverfoereTilSkatt.builder()
        .persistenceService(persistenceService)
        .skattConsumer(skattConsumer)
        .build();
  }

  @Bean
  public VarsleFarOmSigneringsoppgave varsleFarOmSigneringsoppgave(
      @Value("consumer.brukernotifikasjon.antall-dager-forsinkelse-etter-mor-har-signert") int antallDagerForsinkelseEtterMorHarSignert,
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      PersistenceService persistenceService) {
    return VarsleFarOmSigneringsoppgave.builder()
        .antallDagerSidenMorSignerte(antallDagerForsinkelseEtterMorHarSignert)
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .persistenceService(persistenceService)
        .build();
  }

  @Bean
  public SletteOppgave sletteOppgave(
      @Value("consumer.brukernotifikasjon.synlighet.oppgave-antall-dager") int synlighetOppgaveIDager,
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      PersistenceService persistenceService) {
    return SletteOppgave.builder()
        .antallDagerSidenMorSignerte(synlighetOppgaveIDager)
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .persistenceService(persistenceService)
        .build();
  }
}
