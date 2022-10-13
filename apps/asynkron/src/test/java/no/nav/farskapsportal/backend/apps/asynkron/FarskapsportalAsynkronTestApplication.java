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
import java.security.KeyStore;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@EnableAutoConfiguration
@EnableMockOAuth2Server
@ComponentScan("no.nav.farskapsportal.backend")
@SpringBootTest(classes = FarskapsportalAsynkronApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class FarskapsportalAsynkronTestApplication {

  public static final String PROFILE_SKATT_SSL_TEST = "skatt-ssl-test";

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_TEST : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalAsynkronTestApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }

  @Bean
  @Scope("prototype")
  SkattConsumer skattConsumer(@Qualifier("asynk-base") RestTemplate restTemplate, @Value("${url.skatt.base-url}") String baseUrl,
      @Value("${url.skatt.registrering-av-farskap}") String endpoint, ConsumerEndpoint consumerEndpoint) {

    consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, endpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));

    return new SkattConsumer(restTemplate, consumerEndpoint);
  }

  @Bean
  @Profile({PROFILE_TEST, PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES, PROFILE_SCHEDULED_TEST, PROFILE_SKATT_SSL_TEST})
  public KeyStoreConfig keyStoreConfig(@Autowired ResourceLoader resourceLoader) throws IOException {
    try (InputStream inputStream = resourceLoader.getClassLoader().getResourceAsStream("esigneringkeystore.jceks")) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke esigneringkeystore.jceks");
      } else {
        return KeyStoreConfig.fromJavaKeyStore(inputStream, "selfsigned", "changeit", "changeit");
      }
    }
  }

  @Lazy
  @Configuration
  @Profile({PROFILE_SKATT_SSL_TEST, PROFILE_INTEGRATION_TEST})
  public static class SkattStubSslConfiguration {

    @Value("${server.port}")
    private int localServerPort;

    @Value("${sertifikat.passord}")
    private String keystorePassword;

    @Value("${sertifikat.keystore-type}")
    private String keystoreType;

    @Value("${sertifikat.keystore-name}")
    private String keystoreName;

    @Bean
    ServletWebServerApplicationContext servletWebServerApplicationContext() {
      return new ServletWebServerApplicationContext();
    }

    @Bean
    @Scope("prototype")
    @Qualifier(PROFILE_SKATT_SSL_TEST)
    public RestTemplate skattLocalIntegrationRestTemplate(@Qualifier("asynk-base") RestTemplate restTemplate) {

      KeyStore keyStore;
      HttpComponentsClientHttpRequestFactory requestFactory;

      try {
        keyStore = KeyStore.getInstance(keystoreType);
        var classPathResoure = new ClassPathResource(keystoreName);
        var inputStream = classPathResoure.getInputStream();
        keyStore.load(inputStream, keystorePassword.toCharArray());

        var socketFactory = new SSLConnectionSocketFactory(new SSLContextBuilder()
            .loadTrustMaterial(null, new TrustSelfSignedStrategy())
            .loadKeyMaterial(keyStore, keystorePassword.toCharArray()).build(),
            NoopHostnameVerifier.INSTANCE);

        var httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
            .setMaxConnTotal(5)
            .setMaxConnPerRoute(5)
            .build();

        requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(10000);
        requestFactory.setConnectTimeout(10000);

        restTemplate.setRequestFactory(requestFactory);

      } catch (Exception exception) {
        exception.printStackTrace();
      }

      return restTemplate;
    }

    @Bean
    @Qualifier(PROFILE_INTEGRATION_TEST)
    SkattConsumer skattConsumerIntegrationTest(@Qualifier(PROFILE_SKATT_SSL_TEST) RestTemplate restTemplate,
        @Value("${url.skatt.base-url}") String baseUrl,
        @Value("${url.skatt.registrering-av-farskap}") String endpoint, ConsumerEndpoint consumerEndpoint) {
      log.info("Oppretter SkattConsumer med url {}", baseUrl);
      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, endpoint);
      restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
      return new SkattConsumer(restTemplate, consumerEndpoint);
    }

    @Bean
    @Qualifier("sikret")
    SkattConsumer skattConsumerSikret(@Qualifier(PROFILE_SKATT_SSL_TEST) RestTemplate restTemplate,
        @Value("${url.skatt.registrering-av-farskap}") String endpoint, ConsumerEndpoint consumerEndpoint) {

      var baseUrl = "https://localhost:" + localServerPort;

      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, endpoint);
      restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
      return new SkattConsumer(restTemplate, consumerEndpoint);
    }

    @Bean
    @Qualifier("usikret")
    SkattConsumer skattConsumerUsikret(@Qualifier("asynk-base") RestTemplate restTemplate,
        @Value("${url.skatt.registrering-av-farskap}") String endpoint, ConsumerEndpoint consumerEndpoint) {

      var baseUrl = "http://localhost:" + localServerPort;

      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, endpoint);
      restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
      return new SkattConsumer(restTemplate, consumerEndpoint);
    }
  }

  @Bean
  public TokenValidationContextHolder oidcRequestContextHolder() {
    return new SpringTokenValidationContextHolder();
  }
}
