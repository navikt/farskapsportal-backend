package farskapsportal.asynkron;


import static no.nav.farskapsportal.backend.asynkron.FarskapsportalAsynkronApplication.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.backend.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SKATT_SSL_TEST;
import static no.nav.farskapsportal.backend.asynkron.consumer.skatt.SkattEndpointName.MOTTA_FARSKAPSERKLAERING;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;

import java.security.KeyStore;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.farskapsportal.backend.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Profile(PROFILE_TEST)
@Configuration
public class FarskapsportalAsynkronTestConfig {

  public static final String PROFILE_SKATT_SSL_TEST = "skatt-ssl-test";

  @Autowired
  private ServletWebServerApplicationContext webServerAppCtxt;

  @Bean
  SkattConsumer skattConsumer(@Qualifier("base") HttpHeaderRestTemplate restTemplate, @Value("${url.skatt.base-url}") String baseUrl,
      @Value("${url.skatt.registrering-av-farskap}") String endpoint, ConsumerEndpoint consumerEndpoint) {

    var skattBaseUrl = baseUrl + ":" + webServerAppCtxt.getWebServer().getPort();

    consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, endpoint);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(skattBaseUrl));
    return new SkattConsumer(restTemplate, consumerEndpoint);
  }

  @Lazy
  @Configuration
  @Profile(PROFILE_SKATT_SSL_TEST)
  static class SkattStubSslConfiguration {

    @Value("${server.port}")
    private int localServerPort;

    @Value("${sertifikat.passord}")
    private String keystorePassword;

    @Value("${sertifikat.keystore-type}")
    private String keystoreType;

    @Value("${sertifikat.keystore-name}")
    private String keystoreName;

    @Bean
    @Qualifier(PROFILE_SKATT_SSL_TEST)
    public HttpHeaderRestTemplate skattLocalIntegrationRestTemplate(@Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate) {

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

        httpHeaderRestTemplate.setRequestFactory(requestFactory);

      } catch (Exception exception) {
        exception.printStackTrace();
      }

      return httpHeaderRestTemplate;
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
    SkattConsumer skattConsumerUsikret(@Qualifier("base") RestTemplate restTemplate,
        @Value("${url.skatt.registrering-av-farskap}") String endpoint, ConsumerEndpoint consumerEndpoint) {

      var baseUrl = "http://localhost:" + localServerPort;

      consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, endpoint);
      restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(baseUrl));
      return new SkattConsumer(restTemplate, consumerEndpoint);
    }
  }

}
