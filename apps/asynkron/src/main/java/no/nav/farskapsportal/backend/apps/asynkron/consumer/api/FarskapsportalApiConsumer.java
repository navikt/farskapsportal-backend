package no.nav.farskapsportal.backend.apps.asynkron.consumer.api;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;


@Slf4j
@AllArgsConstructor
public class FarskapsportalApiConsumer {

  private final RestTemplate restTemplate;

  private final ConsumerEndpoint consumerEndpoint;

  public void synkronisereSigneringsstatus(int idFarskapserklaering) {

    log.info("Ber om statusoppdatering for farskapserklæring med id {}", idFarskapserklaering);
    var respons = restTemplate.exchange(
        String.format(consumerEndpoint.retrieveEndpoint(
            FarskapsportalApiEndpoint.SYNKRONISERE_SIGNERINGSSTATUS_ENDPOINT_NAME), idFarskapserklaering),
        HttpMethod.PUT,
        new HttpEntity<>(null, null),
        Void.class);

    log.info("Farskapsportal-api svarte med http statuskode {}", respons.getStatusCode());
  }
}
