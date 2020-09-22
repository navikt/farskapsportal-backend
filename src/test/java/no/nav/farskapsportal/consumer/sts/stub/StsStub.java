package no.nav.farskapsportal.consumer.sts.stub;

import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.Kjoenn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
@Component
public class StsStub {

    @Value("${urls.sts.system-oidc-token-endpoint-path}")
    private String hentIdTokenForServicebrukerEndpoint;

    public void runSecurityTokenServiceStub(String mockedIdtoken) {
        stubFor(post(urlEqualTo(hentIdTokenForServicebrukerEndpoint))
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withStatus(201)
                                .withBody(String.join(
                                        "\n",
                                        "{",
                                        " \"access_token\": \"" + mockedIdtoken + "\",",
                                        " \"token_type\": \"Bearer\",",
                                        " \"expires_in\": 5",
                                        "}"
                                ))
                )
        );
    }


}

