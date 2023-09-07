package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattEndpoint.MOTTA_FARSKAPSERKLAERING;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.*;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ResourceLoader;

@Slf4j
@Configuration
@ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(
          type = ASSIGNABLE_TYPE,
          value = {FarskapsportalApiApplication.class})
    })
public class FarskapsportalApiTestConfig {

  public static final String PROFILE_SKATT_SSL_TEST = "skatt-ssl-test";
  private String keyStorePassword = "changeit";
  private String keyStoreName = "esigneringkeystore.jceks";

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
    try (InputStream inputStream =
        resourceLoader.getClassLoader().getResourceAsStream("esigneringkeystore.jceks")) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke esigneringkeystore.jceks");
      } else {
        return KeyStoreConfig.fromJavaKeyStore(inputStream, "selfsigned", "changeit", "changeit");
      }
    }
  }

  @Bean
  @Qualifier("skatt")
  @Profile({
    PROFILE_TEST,
    PROFILE_LOCAL,
    PROFILE_LOCAL_POSTGRES,
    PROFILE_REMOTE_POSTGRES,
    PROFILE_SCHEDULED_TEST
  })
  public no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig keyStoreConfigSkatt(
      @Autowired ResourceLoader resourceLoader) throws IOException {
    return SkattStubSslConfiguration.createKeyStoreConfig(
        resourceLoader, keyStorePassword, keyStoreName);
  }

  @Bean
  @Scope("prototype")
  @Profile(PROFILE_SKATT_SSL_TEST)
  SkattConsumer skattConsumer(
      @Value("${url.skatt.base-url}") String baseUrl,
      @Value("${url.skatt.registrering-av-farskap}") String endpoint,
      ConsumerEndpoint consumerEndpoint) {

    consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, baseUrl + endpoint);

    var httpClient = HttpClients.custom().evictExpiredConnections().build();

    return new SkattConsumer(httpClient, consumerEndpoint);
  }

  @Lazy
  @Configuration
  @Profile({PROFILE_SKATT_SSL_TEST})
  public static class SkattStubSslConfiguration {

    @Value("${server.port}")
    private int localServerPort;

    @Value("${sertifikat.passord}")
    private String keyStorePassword;

    @Value("${sertifikat.keystore-type}")
    private String keyStoreType;

    @Value("${sertifikat.keystore-name}")
    private String keyStoreName;

    public static no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig
        createKeyStoreConfig(
            ResourceLoader resourceLoader, String keyStorePassword, String keyStoreName)
            throws IOException {
      try (InputStream inputStream =
          resourceLoader.getClassLoader().getResourceAsStream(keyStoreName)) {
        if (inputStream == null) {
          throw new IllegalArgumentException("Fant ikke " + keyStoreName);
        } else {
          return no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig
              .fromJavaKeyStore(inputStream, "selfsigned", keyStorePassword, keyStorePassword);
        }
      }
    }

    @Bean
    @Qualifier("skatt")
    public no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig keyStoreConfig(
        @Autowired ResourceLoader resourceLoader) throws IOException {
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
    @Qualifier("sikret")
    SkattConsumer skattConsumerSikret(
        @Value("${url.skatt.registrering-av-farskap}") String endpoint,
        CloseableHttpClient httpClient) {

      var baseUrl = "https://localhost:" + localServerPort;
      var consumerEndpoint = new ConsumerEndpoint();
      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, baseUrl + endpoint);
      return new SkattConsumer(httpClient, consumerEndpoint);
    }

    @Bean
    @Qualifier("usikret")
    SkattConsumer skattConsumerUsikret(
        @Value("${url.skatt.registrering-av-farskap}") String endpoint,
        ConsumerEndpoint consumerEndpoint) {

      var baseUrl = "http://localhost:" + localServerPort;

      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, baseUrl + endpoint);
      var httpClient = HttpClients.custom().evictExpiredConnections().build();
      return new SkattConsumer(httpClient, consumerEndpoint);
    }
  }
}
