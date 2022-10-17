package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_REMOTE_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.Certificates;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.core.Sender;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.stub.DifiESignaturStub;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = {FarskapsportalApiApplication.class})})
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    topics = {"aapen-brukernotifikasjon-nyBeskjed-v1", "aapen-brukernotifikasjon-done-v1", "aapen-brukernotifikasjon-nyOppgave-v1"})
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@Slf4j
@EntityScan("no.nav.farskapsportal.backend.libs.entity")
public class FarskapsportalApiApplicationLocal {

  public static final String PADES = "/pades";
  public static final String XADES = "/xades";
  private static final String NAV_ORGNR = "123456789";

  public static void main(String... args) {

    String profile = args.length < 1 ? PROFILE_LOCAL : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplicationLocal.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }

  @Bean
  @Primary
  @Profile({PROFILE_TEST, PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig, @Value("${url.esignering}") String esigneringUrl)
      throws URISyntaxException {
    return ClientConfiguration.builder(keyStoreConfig).trustStore(Certificates.TEST).serviceUri(new URI(esigneringUrl + "/esignering"))
        .globalSender(new Sender(NAV_ORGNR)).build();
  }


  @Configuration
  @Profile({PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
  static class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(@Qualifier("dataSource") DataSource dataSource, @Value("${spring.flyway.placeholders.user}") String dbUserAsynkron) {

      var placeholders = new HashMap<String, String>();
      placeholders.put("user_asynkron", dbUserAsynkron);

      Flyway.configure().ignoreMissingMigrations(true).baselineOnMigrate(true).dataSource(dataSource).placeholders(placeholders).load().migrate();
    }
  }

  @Configuration
  @Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES, PROFILE_INTEGRATION_TEST})
  @EnableMockOAuth2Server
  @AutoConfigureWireMock(port = 0)
  class MockOauthServerLocalConfig {

    @Autowired
    private DifiESignaturStub difiESignaturStub;

    @Bean
    public void runStubs() {
      difiESignaturStub.runGetSignedDocument(PADES);
      difiESignaturStub.runGetXades(XADES);
    }
  }
}
