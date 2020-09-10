package no.nav.farskapsportal.consumer.sts.nav;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import no.nav.farskapsportal.consumer.sts.TokenSupplier;
import no.nav.farskapsportal.consumer.sts.TokenWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.RequestEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class NavStsTokenSupplier implements TokenSupplier {
    private final RestTemplate restTemplate;
    private final String url;
    private final String username;
    private final String password;

    @Override
    @Retryable(maxAttempts = 10, backoff = @Backoff(300))
    public TokenWrapper fetchToken() {
        val entity = RequestEntity
                .get(URI.create(url))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8)))
                .build();

        val result = restTemplate.exchange(entity, JsonNode.class).getBody();
        val idToken = result.get("access_token").asText();
        val expiry = LocalDateTime.now().plusSeconds(result.get("expires_in").asLong());
        return new TokenWrapper(idToken, expiry);
    }
}