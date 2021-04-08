package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.config.FarskapsportalConfig.X_API_KEY;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.farskapsportal.consumer.sts.SecurityTokenServiceConsumer;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@Slf4j
public class RestTemplateConfig {

  private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";
  private static final String TEMA = "Tema";
  private static final String TEMA_FAR = "FAR";

  @Bean
  @Qualifier("base")
  @Scope("prototype")
  public HttpHeaderRestTemplate restTemplate() {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();
    httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter::fetchCorrelationIdForThread);
    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("sts")
  @Scope("prototype")
  public HttpHeaderRestTemplate stsRestTemplate(@Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${APIKEY_STS_FP}") String xApiKeySts) {
    log.info("Setter {} for STS", X_API_KEY);
    httpHeaderRestTemplate.addHeaderGenerator(X_API_KEY, () -> xApiKeySts);
    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("skatt")
  @Scope("prototype")
  public HttpHeaderRestTemplate skattRestTemplate(@Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate) {
      // TODO: fyll inn med virksomhetssertifikat fra GCP secret
    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("pdl-api")
  @Scope("prototype")
  public HttpHeaderRestTemplate pdlApiRestTemplate(@Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${farskapsportal-api.servicebruker.brukernavn}") String farskapsportalApiBrukernavn,
      @Value("${farskapsportal-api.servicebruker.passord}") String farskapsportalApiPassord, @Value("${APIKEY_PDLAPI_FP}") String xApiKeyPdlApi,
      @Autowired SecurityTokenServiceConsumer securityTokenServiceConsumer) {

    httpHeaderRestTemplate.addHeaderGenerator(AUTHORIZATION,
        () -> "Bearer " + securityTokenServiceConsumer.hentIdTokenForServicebruker(farskapsportalApiBrukernavn, farskapsportalApiPassord));

    httpHeaderRestTemplate.addHeaderGenerator(NAV_CONSUMER_TOKEN,
        () -> "Bearer " + securityTokenServiceConsumer.hentIdTokenForServicebruker(farskapsportalApiBrukernavn, farskapsportalApiPassord));

    httpHeaderRestTemplate.addHeaderGenerator(TEMA, () -> TEMA_FAR);

    log.info("Setter {} for pdl-api", X_API_KEY);
    Validate.isTrue(xApiKeyPdlApi != null);
    Validate.isTrue(!xApiKeyPdlApi.isBlank());

    httpHeaderRestTemplate.addHeaderGenerator(X_API_KEY, () -> xApiKeyPdlApi);
    return httpHeaderRestTemplate;
  }
}
