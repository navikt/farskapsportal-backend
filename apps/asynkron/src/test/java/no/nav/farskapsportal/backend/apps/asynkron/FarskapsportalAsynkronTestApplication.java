package no.nav.farskapsportal.backend.apps.asynkron;

import static no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SCHEDULED_TEST;
import static no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt.SkattEndpoint.MOTTA_FARSKAPSERKLAERING;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_REMOTE_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;

@Slf4j
@EnableAutoConfiguration
@EnableMockOAuth2Server
@ComponentScan("no.nav.farskapsportal.backend")
@SpringBootTest(
    classes = FarskapsportalAsynkronApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class FarskapsportalAsynkronTestApplication {

  public static final String PROFILE_SKATT_SSL_TEST = "skatt-ssl-test";

  private String keyStorePassword = "changeit";

  private String keyStoreName = "esigneringkeystore.jceks";

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_TEST : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalAsynkronTestApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }

  @Bean
  @Scope("prototype")
  SkattConsumer skattConsumer(
      @Value("${url.skatt.base-url}") String baseUrl,
      @Value("${url.skatt.registrering-av-farskap}") String endpoint,
      ConsumerEndpoint consumerEndpoint) {

    consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, baseUrl + endpoint);

    var httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create().build();

    return new SkattConsumer(httpClientConnectionManager, consumerEndpoint);
  }

  @Bean
  @Profile({
    PROFILE_TEST,
    PROFILE_LOCAL,
    PROFILE_LOCAL_POSTGRES,
    PROFILE_REMOTE_POSTGRES,
    PROFILE_SCHEDULED_TEST
  })
  public KeyStoreConfig keyStoreConfig(@Autowired ResourceLoader resourceLoader)
      throws IOException {
    return SkattStubSslConfiguration.createKeyStoreConfig(
        resourceLoader, keyStorePassword, keyStoreName);
  }

  @Lazy
  @Configuration
  @Profile({PROFILE_SKATT_SSL_TEST, PROFILE_INTEGRATION_TEST})
  public static class SkattStubSslConfiguration {

    @Value("${server.port}")
    private int localServerPort;

    @Value("${sertifikat.passord}")
    private String keyStorePassword;

    @Value("${sertifikat.keystore-type}")
    private String keyStoreType;

    @Value("${sertifikat.keystore-name}")
    private String keyStoreName;

    public static KeyStoreConfig createKeyStoreConfig(
        ResourceLoader resourceLoader, String keyStorePassword, String keyStoreName)
        throws IOException {
      try (InputStream inputStream =
          resourceLoader.getClassLoader().getResourceAsStream(keyStoreName)) {
        if (inputStream == null) {
          throw new IllegalArgumentException("Fant ikke " + keyStoreName);
        } else {
          return KeyStoreConfig.fromJavaKeyStore(
              inputStream, "selfsigned", keyStorePassword, keyStorePassword);
        }
      }
    }

    @Bean
    public KeyStoreConfig keyStoreConfig(@Autowired ResourceLoader resourceLoader)
        throws IOException {
      return SkattStubSslConfiguration.createKeyStoreConfig(
          resourceLoader, keyStorePassword, keyStoreName);
    }

    @Bean
    ServletWebServerApplicationContext servletWebServerApplicationContext() {
      return new ServletWebServerApplicationContext();
    }

    @Bean
    public TokenValidationContextHolder oidcRequestContextHolder() {
      return new SpringTokenValidationContextHolder();
    }

    @Bean
    @Qualifier(PROFILE_INTEGRATION_TEST)
    SkattConsumer skattConsumerIntegrationTest(
        @Value("${url.skatt.base-url}") String baseUrl,
        @Value("${url.skatt.registrering-av-farskap}") String endpoint,
        PoolingHttpClientConnectionManager httpClientConnectionManager) {
      log.info("Oppretter SkattConsumer med url {}", baseUrl);
      var consumerEndpoint = new ConsumerEndpoint();
      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, baseUrl + endpoint);

      return new SkattConsumer(httpClientConnectionManager, consumerEndpoint);
    }

    @Bean
    @Qualifier("sikret")
    SkattConsumer skattConsumerSikret(
        @Value("${url.skatt.registrering-av-farskap}") String endpoint,
        PoolingHttpClientConnectionManager httpClientConnectionManagerSslTest) {

      var baseUrl = "https://localhost:" + localServerPort;
      var consumerEndpoint = new ConsumerEndpoint();
      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, baseUrl + endpoint);
      return new SkattConsumer(httpClientConnectionManagerSslTest, consumerEndpoint);
    }

    @Bean
    @Qualifier("usikret")
    SkattConsumer skattConsumerUsikret(
        @Value("${url.skatt.registrering-av-farskap}") String endpoint,
        ConsumerEndpoint consumerEndpoint) {

      var baseUrl = "http://localhost:" + localServerPort;

      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, baseUrl + endpoint);
      var httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create().build();
      return new SkattConsumer(httpClientConnectionManager, consumerEndpoint);
    }
  }
}
