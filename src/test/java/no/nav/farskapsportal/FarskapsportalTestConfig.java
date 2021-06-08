package no.nav.farskapsportal;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.consumer.skatt.SkattEndpointName.MOTTA_FARSKAPSERKLAERING;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.skatt.SkattConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(PROFILE_TEST)
@Configuration
public class FarskapsportalTestConfig {

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

}
