package no.nav.farskapsportal.backend.apps.api.config;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_SCHEDULED_TEST;

import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.api.scheduled.arkiv.ArkivereFarskapserklaeringer;
import no.nav.farskapsportal.backend.apps.api.scheduled.arkiv.DeaktivereFarskapserklaeringer;
import no.nav.farskapsportal.backend.apps.api.scheduled.brukernotifikasjon.Brukernotifikasjonstyring;
import no.nav.farskapsportal.backend.apps.api.scheduled.brukernotifikasjon.Varsel;
import no.nav.farskapsportal.backend.apps.api.scheduled.esignering.OppdatereSigneringsstatus;
import no.nav.farskapsportal.backend.apps.api.scheduled.oppgave.Oppgavestyring;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.farskapsportal.backend.apps.api.service.PersonopplysningService;
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

  public ScheduledConfig(
      @Autowired FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper) {
    this.farskapsportalAsynkronEgenskaper = farskapsportalAsynkronEgenskaper;
  }

  @Bean
  public ArkivereFarskapserklaeringer arkivereFarskapserklaeringer(
      DifiESignaturConsumer difiEsignerinConsumer,
      PersistenceService persistenceService,
      SkattConsumer skattConsumer) {

    return ArkivereFarskapserklaeringer.builder()
        .intervallMellomForsoek(
            farskapsportalAsynkronEgenskaper.getArkiv().getArkiveringsintervall())
        .difiESignaturConsumer(difiEsignerinConsumer)
        .persistenceService(persistenceService)
        .skattConsumer(skattConsumer)
        .build();
  }

  @Bean
  public DeaktivereFarskapserklaeringer deaktivereFarskapserklaeringer(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      PersistenceService persistenceService) {
    return DeaktivereFarskapserklaeringer.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .egenskaperArkiv(farskapsportalAsynkronEgenskaper.getArkiv())
        .persistenceService(persistenceService)
        .build();
  }

  @Bean
  public OppdatereSigneringsstatus oppdatereSigneringsstatus(
      FarskapsportalService farskapsportalService, PersistenceService persistenceService) {

    return OppdatereSigneringsstatus.builder()
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .farskapsportalService(farskapsportalService)
        .persistenceService(persistenceService)
        .build();
  }

  @Bean
  public Brukernotifikasjonstyring brukernotifikasjonstyring(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      FarskapserklaeringDao farskapserklaeringDao,
      PersistenceService persistenceService) {

    return Brukernotifikasjonstyring.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapserklaeringDao(farskapserklaeringDao)
        .persistenceService(persistenceService)
        .build();
  }

  @Bean
  public Varsel varsel(
      BrukernotifikasjonConsumer brukernotifikasjonConsumer,
      PersistenceService persistenceService) {

    return Varsel.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .egenskaperBrukernotifikasjon(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon())
        .persistenceService(persistenceService)
        .build();
  }

  @Bean
  public Oppgavestyring oppgavestyring(
      FarskapserklaeringDao farskapserklaeringDao,
      OppgaveApiConsumer oppgaveApiConsumer,
      PersonopplysningService personopplysningService) {
    return Oppgavestyring.builder()
        .egenskaperOppgavestyring(farskapsportalAsynkronEgenskaper.getOppgave())
        .farskapserklaeringDao(farskapserklaeringDao)
        .oppgaveApiConsumer(oppgaveApiConsumer)
        .personopplysningService(personopplysningService)
        .build();
  }
}
