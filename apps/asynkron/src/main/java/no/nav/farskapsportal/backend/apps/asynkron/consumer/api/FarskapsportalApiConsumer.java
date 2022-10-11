package no.nav.farskapsportal.backend.apps.asynkron.consumer.api;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.dto.asynkroncontroller.HenteAktoeridRequest;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Slf4j
@AllArgsConstructor
public class FarskapsportalApiConsumer {

  public static final Logger SIKKER_LOGG = LoggerFactory.getLogger("secureLogger");
  private final RestTemplate restTemplate;
  private final String baseUrl;
  private final ConsumerEndpoint consumerEndpoint;

  @Retryable(value = RestClientException.class, maxAttempts = 10, backoff = @Backoff(delay = 30000))
  public Optional<String> henteAktoerid(String personident) {
    log.info("Henter aktørid for person.");
    SIKKER_LOGG.info("Henter aktørid for personident {}", personident);

    var henteAktoeridRequest = HenteAktoeridRequest.builder().personident(personident).build();
    ResponseEntity<String> respons = null;

    try {
      respons = restTemplate.exchange(
          baseUrl + consumerEndpoint.retrieveEndpoint(FarskapsportalApiEndpoint.HENTE_AKTOERID_ENDPOINT_NAME),
          HttpMethod.POST, new HttpEntity<>(henteAktoeridRequest), String.class);
    } catch (RestClientException rce) {
      log.warn("Kall mot farskapsportal-api for henting av aktørid feilet med http-statuskokde {}", respons.getStatusCode());
      SIKKER_LOGG.warn("Kall mot farskapsportal-api for henting av aktørid  for personident {} feilet med http-statuskokde {}",
          henteAktoeridRequest.getPersonident(), respons.getStatusCode());
      throw rce;
    }

    log.info("Mottok {}-respons fra farskapsportal-api", respons.getStatusCode());
    SIKKER_LOGG.info("Fant aktørid {} for personident {}", respons.getBody(), henteAktoeridRequest.getPersonident());

    return HttpStatus.OK.equals(respons.getStatusCode()) && respons.getBody() != null && !respons.getBody().isBlank()
        ? Optional.of(respons.getBody()) : Optional.empty();
  }

  public void synkronisereSigneringsstatus(int idFarskapserklaering) {

    log.info("Ber om statusoppdatering for farskapserklæring med id {}", idFarskapserklaering);

    log.info("Endepunkt oppdatere signeringsstatus: {}", String.format(consumerEndpoint.retrieveEndpoint(
        FarskapsportalApiEndpoint.SYNKRONISERE_SIGNERINGSSTATUS_ENDPOINT_NAME), idFarskapserklaering));

    var respons = restTemplate.exchange(
        String.format(consumerEndpoint.retrieveEndpoint(
            FarskapsportalApiEndpoint.SYNKRONISERE_SIGNERINGSSTATUS_ENDPOINT_NAME), idFarskapserklaering),
        HttpMethod.PUT,
        new HttpEntity<>(null, null),
        Void.class);

    log.info("Farskapsportal-api svarte med http statuskode {}", respons.getStatusCode());
  }
}
