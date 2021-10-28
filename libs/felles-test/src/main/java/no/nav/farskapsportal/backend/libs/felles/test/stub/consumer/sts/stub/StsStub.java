package no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.sts.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StsStub {

  @Value("${url.sts.security-token-service}")
  private String hentIdTokenForServicebrukerEndpoint;

  public void runSecurityTokenServiceStub(String mockedIdtoken) {
    stubFor(
        post(urlEqualTo(hentIdTokenForServicebrukerEndpoint))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withStatus(201)
                    .withBody(
                        String.join(
                            "\n",
                            "{",
                            " \"access_token\": \"" + mockedIdtoken + "\",",
                            " \"token_type\": \"Bearer\",",
                            " \"expires_in\": 5",
                            "}"))));
  }
}
