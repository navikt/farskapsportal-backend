package no.nav.farskapsportal.backend.libs.felles.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.*;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@Configuration
@EnableAutoConfiguration
@ComponentScan("no.nav.farskapsportal.backend")
@EntityScan("no.nav.farskapsportal.backend.libs.entity")
@EnableJpaRepositories("no.nav.farskapsportal.backend.libs.felles.persistence.dao")
@Import({BrukernotifikasjonConfig.class, RestTemplateFellesConfig.class})
public class FarskapsportalFellesConfig {

  public static final Logger SIKKER_LOGG = LoggerFactory.getLogger("secureLogger");

  public static final String PROFILE_LIVE = "live";
  public static final String PROFILE_LOCAL = "local";
  public static final String PROFILE_LOCAL_NAIS = "local-nais";
  public static final String PROFILE_INTEGRATION_TEST = "integration-test";
  public static final String PROFILE_TEST = "test";
  public static final String PROFILE_LOCAL_POSTGRES = "local-postgres";
  public static final String PROFILE_REMOTE_POSTGRES = "remote-postgres";
  public static final String PROFILE_SCHEDULED_TEST = "scheduled-test";
  public static String KODE_LAND_NORGE = "NOR";

  @Bean
  public PersistenceService persistenceService(
      BucketConsumer bucketConsumer,
      OppgavebestillingDao oppgavebestillingDao,
      FarskapserklaeringDao farskapserklaeringDao,
      @Autowired ModelMapper modelMapper,
      BarnDao barnDao,
      ForelderDao forelderDao,
      StatusKontrollereFarDao kontrollereFarDao,
      MeldingsloggDao meldingsloggDao) {

    return new PersistenceService(
        bucketConsumer,
        oppgavebestillingDao,
        farskapserklaeringDao,
        barnDao,
        forelderDao,
        kontrollereFarDao,
        meldingsloggDao,
        modelMapper);
  }

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }
}
