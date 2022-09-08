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

  @Autowired
  private MockOAuth2Server mockOAuth2Server;

  private String generateTestToken() {
    var token = mockOAuth2Server.issueToken("selvbetjening", "aud-localhost", "aud-localhost");
    return "Bearer " + token.serialize();
  }

  @Bean
  HttpHeaderTestRestTemplate httpHeaderTestRestTemplate() {
    TestRestTemplate testRestTemplate = new TestRestTemplate(new RestTemplateBuilder());
    HttpHeaderTestRestTemplate httpHeaderTestRestTemplate = new HttpHeaderTestRestTemplate(testRestTemplate);
    httpHeaderTestRestTemplate.add(HttpHeaders.AUTHORIZATION, this::generateTestToken);

    return httpHeaderTestRestTemplate;
  }
}
