package no.nav.farskapsportal.backend.libs.felles.consumer.sts;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@AllArgsConstructor
@Slf4j
public class SecurityTokenServiceConsumer {

  private final RestTemplate restTemplate;

  private final ConsumerEndpoint consumerEndpoint;

  private static final MultiValueMap<String, String> PARAMETERS =
      new LinkedMultiValueMap<>(2) {
        {
          add("grant_type", "client_credentials");
          add("scope", "openid");
        }
      };

  @Retryable(value = Exception.class, backoff = @Backoff(delay = 500))
  public String hentIdTokenForServicebruker(String brukernavn, String passord) {

    Validate.isTrue(brukernavn != null);
    Validate.isTrue(passord != null);

    log.info("Henter id-token for servicebruker {}", brukernavn);

    var headers = new HttpHeaders();
    headers.put(
        HttpHeaders.AUTHORIZATION,
        List.of("Basic " + base64EncodeCredentials(brukernavn, passord)));

    var response =
        restTemplate.exchange(
            consumerEndpoint.retrieveEndpoint(
                SecurityTokenServiceEndpointName.HENTE_IDTOKEN_FOR_SERVICEUSER),
            HttpMethod.POST,
            new HttpEntity<>(PARAMETERS, headers),
            SecurityTokenServiceResponse.class);

    log.info("Respons fra STS: {}", response);

    var securityTokenServiceResponse = response.getBody();

    return Optional.ofNullable(securityTokenServiceResponse)
        .map(SecurityTokenServiceResponse::getIdToken)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    String.format(
                        "Kunne ikke hente token fra '%s', response: %s",
                        consumerEndpoint, response.getStatusCode())));
  }

  private static String base64EncodeCredentials(String username, String password) {
    String credentials = username + ":" + password;
    byte[] encodedCredentials = Base64.getEncoder().encode(credentials.getBytes());
    return new String(encodedCredentials, StandardCharsets.UTF_8);
  }
}
