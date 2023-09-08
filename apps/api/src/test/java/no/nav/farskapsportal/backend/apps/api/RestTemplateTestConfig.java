package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_REMOTE_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;

import com.google.common.net.HttpHeaders;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({PROFILE_TEST, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
public class RestTemplateTestConfig {

  @Autowired private MockOAuth2Server mockOAuth2Server;

  private String generateTestToken(Farskapsportalapp farskapsportalapp) {

    var issuerid = farskapsportalapp.equals(Farskapsportalapp.API) ? "selvbetjening" : "aad";

    var token = mockOAuth2Server.issueToken(issuerid, "aud-localhost", "aud-localhost");
    return "Bearer " + token.serialize();
  }

  @Bean("api")
  HttpHeaderTestRestTemplate httpHeaderTestRestTemplateApi() {
    TestRestTemplate testRestTemplate = new TestRestTemplate(new RestTemplateBuilder());
    HttpHeaderTestRestTemplate httpHeaderTestRestTemplate =
        new HttpHeaderTestRestTemplate(testRestTemplate);
    httpHeaderTestRestTemplate.add(
        HttpHeaders.AUTHORIZATION, () -> generateTestToken(Farskapsportalapp.API));

    return httpHeaderTestRestTemplate;
  }

  public enum Farskapsportalapp {
    API,
    ASYNKRON
  }
}
