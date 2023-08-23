package no.nav.farskapsportal.backend.libs.felles.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Slf4j
@Configuration
@Qualifier("felles")
public class RestTemplateFellesConfig {

  public static final String X_API_KEY = "x-nav-apiKey";

  @Bean("base")
  @Scope("prototype")
  public HttpHeaderRestTemplate restTemplate() {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate();
    httpHeaderRestTemplate.addHeaderGenerator(
        CorrelationIdFilter.CORRELATION_ID_HEADER,
        CorrelationIdFilter::fetchCorrelationIdForThread);
    return httpHeaderRestTemplate;
  }
}
