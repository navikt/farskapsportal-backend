package no.nav.farskapsportal.backend.libs.felles.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.backend.libs.felles.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.pdl.PdlApiConsumerEndpointName;
import no.nav.farskapsportal.backend.libs.felles.consumer.sts.SecurityTokenServiceConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.sts.SecurityTokenServiceEndpointName;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.BarnDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

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
  public static final String PROFILE_INTEGRATION_TEST = "integration-test";
  public static final String PROFILE_TEST = "test";
  public static final String PROFILE_LOCAL_POSTGRES = "local-postgres";
  public static final String PROFILE_REMOTE_POSTGRES = "remote-postgres";
  public static String KODE_LAND_NORGE = "NOR";

  @Bean
  public PdlApiConsumer pdlApiConsumer(@Qualifier("pdl-api") RestTemplate restTemplate,
      @Value("${url.pdl-api.base-url}") String baseUrl,
      @Value("${url.pdl-api.graphql}") String pdlApiEndpoint,
      ConsumerEndpoint consumerEndpoint) {
    consumerEndpoint.addEndpoint(PdlApiConsumerEndpointName.PDL_API_GRAPHQL, pdlApiEndpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    log.info("Oppretter PdlApiConsumer med url {}", baseUrl);
    return PdlApiConsumer.builder().restTemplate(restTemplate).consumerEndpoint(consumerEndpoint).build();
  }


  @Bean
  SecurityTokenServiceConsumer securityTokenServiceConsumer(@Qualifier("sts") RestTemplate restTemplate,
      @Value("${url.sts.base-url}") String baseUrl,
      @Value("${url.sts.security-token-service}") String endpoint,
      ConsumerEndpoint consumerEndpoint) {
    log.info("Oppretter SecurityTokenServiceConsumer med url {}", baseUrl);
    consumerEndpoint.addEndpoint(SecurityTokenServiceEndpointName.HENTE_IDTOKEN_FOR_SERVICEUSER, endpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
    return new SecurityTokenServiceConsumer(restTemplate, consumerEndpoint);
  }

  @Bean
  public PersistenceService persistenceService(
      OppgavebestillingDao oppgavebestillingDao,
      FarskapserklaeringDao farskapserklaeringDao,
      @Autowired ModelMapper modelMapper,
      BarnDao barnDao,
      ForelderDao forelderDao,
      StatusKontrollereFarDao kontrollereFarDao,
      MeldingsloggDao meldingsloggDao) {

    return new PersistenceService(
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
