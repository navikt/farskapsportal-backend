package no.nav.farskapsportal.backend.apps.api.config;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.security.service.SecurityTokenService;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@ComponentScan("no.nav.farskapsportal")
public class RestTemplateConfig {
  private static final String BEHANDLINGSNUMMER = "Behandlingsnummer";
  private static final String BEHANDLINGSNUMMER_FARSKAP = "B145";
  private static final String TEMA_FAR = "FAR";
  private static final String TEMA = "Tema";
  private static final String CLIENT_FARSKAPSPORTAL_API = "farskapsportal-api";
  private static final String CLIENT_PDL_API = "pdl-api";
  private static final String CLIENT_OPPGAVE_API = "oppgave-api";
  public static final String X_CORRELATION_ID_HEADER_NAME = "X-Correlation-ID";

  private @Autowired SecurityTokenService securityTokenService;

  @Bean
  @Scope("prototype")
  @Qualifier("farskapsportal-api")
  public RestTemplate farskapsportalApiRestTemplate(
      @Value("${url.farskapsportal.api.base-url}") String farskapsportalApiRootUrl,
      ClientConfigurationProperties clientConfigurationProperties,
      OAuth2AccessTokenService oAuth2AccessTokenService) {

    var restTemplate = new RestTemplate();

    ClientProperties clientProperties =
        Optional.ofNullable(
                clientConfigurationProperties.getRegistration().get(CLIENT_FARSKAPSPORTAL_API))
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "fant ikke oauth2-klientkonfig for " + CLIENT_FARSKAPSPORTAL_API));

    restTemplate
        .getInterceptors()
        .add(accessTokenInterceptor(clientProperties, oAuth2AccessTokenService));
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(farskapsportalApiRootUrl));

    return restTemplate;
  }

  @Bean
  @Scope("prototype")
  @Qualifier("oppgave")
  public RestTemplate oppgaveRestTemplate(
      @Value("${url.oppgave.base-url}") String oppgaveRootUrl,
      ClientConfigurationProperties clientConfigurationProperties,
      OAuth2AccessTokenService oAuth2AccessTokenService) {

    ClientProperties clientProperties =
        Optional.ofNullable(clientConfigurationProperties.getRegistration().get(CLIENT_OPPGAVE_API))
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "fant ikke oauth2-klientkonfig for " + CLIENT_OPPGAVE_API));

    var restTemplate = new RestTemplate();
    restTemplate
        .getInterceptors()
        .add(accessTokenInterceptor(clientProperties, oAuth2AccessTokenService));

    log.info("Oppretter oppgaveRestTemplate med baseurl {}", oppgaveRootUrl);
    restTemplate.setUriTemplateHandler(new RootUriTemplateHandler(oppgaveRootUrl));

    return restTemplate;
  }

  @Bean("pdl-api")
  @Scope("prototype")
  public HttpHeaderRestTemplate pdlApiRestTemplate(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate) {
    httpHeaderRestTemplate.addHeaderGenerator(
        HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON_VALUE);
    httpHeaderRestTemplate.addHeaderGenerator(BEHANDLINGSNUMMER, () -> BEHANDLINGSNUMMER_FARSKAP);
    httpHeaderRestTemplate.addHeaderGenerator(TEMA, () -> TEMA_FAR);

    httpHeaderRestTemplate
        .getInterceptors()
        .add(securityTokenService.clientCredentialsTokenInterceptor(CLIENT_PDL_API));
    return httpHeaderRestTemplate;
  }

  private ClientHttpRequestInterceptor accessTokenInterceptor(
      ClientProperties clientProperties, OAuth2AccessTokenService oAuth2AccessTokenService) {
    return (request, body, execution) -> {
      OAuth2AccessTokenResponse response =
          oAuth2AccessTokenService.getAccessToken(clientProperties);
      request.getHeaders().setBearerAuth(response.getAccessToken());
      return execution.execute(request, body);
    };
  }
}
