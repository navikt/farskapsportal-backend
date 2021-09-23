package no.nav.farskapsportal.backend.lib.felles.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.lib.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.lib.felles.config.tls.KeyStoreConfig;
import no.nav.farskapsportal.backend.lib.felles.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.backend.lib.felles.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.lib.felles.consumer.pdl.PdlApiConsumerEndpointName;
import no.nav.farskapsportal.backend.lib.felles.consumer.sts.SecurityTokenServiceConsumer;
import no.nav.farskapsportal.backend.lib.felles.consumer.sts.SecurityTokenServiceEndpointName;
import no.nav.farskapsportal.backend.lib.felles.gcp.secretmanager.AccessSecretVersion;
import no.nav.farskapsportal.backend.lib.felles.gcp.secretmanager.FarskapKeystoreCredentials;
import no.nav.farskapsportal.backend.lib.felles.persistence.dao.BarnDao;
import no.nav.farskapsportal.backend.lib.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.lib.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.lib.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.lib.felles.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.backend.lib.felles.service.PersistenceService;
import no.nav.farskapsportal.backend.lib.felles.service.PersonopplysningService;
import no.nav.farskapsportal.backend.lib.felles.util.Mapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@EnableAutoConfiguration
@ComponentScan("no.nav.farskapsportal.backend")
@EntityScan("no.nav.farskapsportal.backend.lib.entity")
@EnableJpaRepositories("no.nav.farskapsportal.backend.lib.felles.persistence.dao")
public class FarskapsportalFellesConfig {

  public static final String PROFILE_LIVE = "live";
  public static final String PROFILE_INTEGRATION_TEST = "integration-test";
  public static final String PROFILE_SCHEDULED_TEST = "scheduled-test";
  public static final String PROFILE_TEST = "test";
  public static String KODE_LAND_NORGE = "NOR";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("bearer-key", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
        ).info(new io.swagger.v3.oas.models.info.Info().title("farskapsportal-api").version("v1"));
  }

  @Bean
  @Profile({PROFILE_LIVE, PROFILE_INTEGRATION_TEST})
  public KeyStoreConfig keyStoreConfig(
      @Value("${virksomhetssertifikat.prosjektid}") String virksomhetssertifikatProsjektid,
      @Value("${virksomhetssertifikat.hemmelighetnavn}") String virksomhetssertifikatHemmelighetNavn,
      @Value("${virksomhetssertifikat.hemmelighetversjon}") String virksomhetssertifikatHemmelighetVersjon,
      @Value("${virksomhetssertifikat.passord.prosjektid}") String virksomhetssertifikatPassordProsjektid,
      @Value("${virksomhetssertifikat.passord.hemmelighetnavn}") String virksomhetssertifikatPassordHemmelighetNavn,
      @Value("${virksomhetssertifikat.passord.hemmelighetversjon}") String virksomhetssertifikatPassordHemmelighetVersjon,
      @Autowired(required = false) AccessSecretVersion accessSecretVersion) throws IOException {

    var sertifikatpassord = accessSecretVersion
        .accessSecretVersion(virksomhetssertifikatPassordProsjektid, virksomhetssertifikatPassordHemmelighetNavn,
            virksomhetssertifikatPassordHemmelighetVersjon).getData().toStringUtf8();

    var objectMapper = new ObjectMapper();
    var farskapKeystoreCredentials = objectMapper.readValue(sertifikatpassord, FarskapKeystoreCredentials.class);

    log.info("lengde sertifikatpassord {}", farskapKeystoreCredentials.getPassword().length());

    var secretPayload = accessSecretVersion
        .accessSecretVersion(virksomhetssertifikatProsjektid, virksomhetssertifikatHemmelighetNavn, virksomhetssertifikatHemmelighetVersjon);

    log.info("lengde sertifikat: {}", secretPayload.getData().size());
    var inputStream = new ByteArrayInputStream(secretPayload.getData().toByteArray());

    return KeyStoreConfig
        .fromJavaKeyStore(inputStream, farskapKeystoreCredentials.getAlias(), farskapKeystoreCredentials.getPassword(),
            farskapKeystoreCredentials.getPassword());
  }

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
  public PersistenceService persistenceService(PersonopplysningService personopplysningService,
      FarskapserklaeringDao farskapserklaeringDao,
      Mapper mapper,
      BarnDao barnDao,
      ForelderDao forelderDao,
      StatusKontrollereFarDao kontrollereFarDao,
      MeldingsloggDao meldingsloggDao) {
    return new PersistenceService(personopplysningService, farskapserklaeringDao, barnDao, forelderDao, kontrollereFarDao,
        meldingsloggDao, mapper);
  }

  @Bean
  public PersonopplysningService personopplysningService(ModelMapper modelMapper, PdlApiConsumer pdlApiConsumer,
      FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper) {
    return PersonopplysningService.builder()
        .modelMapper(modelMapper)
        .pdlApiConsumer(pdlApiConsumer)
        .farskapsportalFellesEgenskaper(farskapsportalFellesEgenskaper).build();
  }

  @Bean
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }
}
