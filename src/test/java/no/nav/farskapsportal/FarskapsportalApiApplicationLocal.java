package no.nav.farskapsportal;

import static no.nav.farskapsportal.FarskapsportalApiApplicationLocal.PROFILE_TEST;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.common.net.HttpHeaders;
import java.io.IOException;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import no.nav.security.token.support.test.jersey.TestTokenGeneratorResource;
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootApplication
@ComponentScan(
    excludeFilters = {
      @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = FarskapsportalApiApplication.class)
    })
@EnableJwtTokenValidation(
    ignore = {"springfox.documentation.swagger.web.ApiResourceController", "org.springframework"})
@Import(TokenGeneratorConfiguration.class)
public class FarskapsportalApiApplicationLocal {

  public static final String PROFILE_LOCAL = "local";
  public static final String PROFILE_TEST = "test";

  public static void main(String... args) {

    String profile = args.length < 1 ? PROFILE_LOCAL : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplicationLocal.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }

  @Bean
  HttpHeaderTestRestTemplate httpHeaderTestRestTemplate() {
    TestRestTemplate testRestTemplate = new TestRestTemplate(new RestTemplateBuilder());
    HttpHeaderTestRestTemplate httpHeaderTestRestTemplate =
        new HttpHeaderTestRestTemplate(testRestTemplate);
    httpHeaderTestRestTemplate.add(
        HttpHeaders.AUTHORIZATION, FarskapsportalApiApplicationLocal::generateTestToken);

    return httpHeaderTestRestTemplate;
  }

  private static String generateTestToken() {
    TestTokenGeneratorResource testTokenGeneratorResource = new TestTokenGeneratorResource();
    return "Bearer " + testTokenGeneratorResource.issueToken("localhost-idtoken");
  }
}
