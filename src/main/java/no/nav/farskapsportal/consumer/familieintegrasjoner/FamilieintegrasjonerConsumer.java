package no.nav.farskapsportal.consumer.familieintegrasjoner;

import static no.nav.farskapsportal.consumer.familieintegrasjoner.FamilieIntegrasjonerEndpointName.POSTSTED;

import lombok.RequiredArgsConstructor;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class FamilieintegrasjonerConsumer {

  private final RestTemplate restTemplate;

  private final ConsumerEndpoint consumerEndpoint;

  @Cacheable("poststed")
  public String hentePoststed(int postnummer) {
    var path = String.format(consumerEndpoint.retrieveEndpoint(POSTSTED), postnummer);
    var respons = restTemplate.exchange(path, HttpMethod.GET, null, String.class);
    return respons.getBody();
  }
}
