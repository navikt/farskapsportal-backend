package no.nav.farskapsportal.consumer.pdl;

import static no.nav.farskapsportal.consumer.pdl.PdlApiConsumerEndpointName.PDL_API_GRAPHQL;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Builder
public class PdlApiHelsesjekkConsumer {

  @NonNull private final RestTemplate restTemplate;
  @NonNull private final ConsumerEndpoint consumerEndpoint;

  public HttpResponse<Boolean> pdlApiGraphqlErILive() {

    var respons = restTemplate.optionsForAllow(consumerEndpoint.retrieveEndpoint(PDL_API_GRAPHQL));

    Validate.isTrue(respons.size() == 2);
    Validate.isTrue(respons.contains(HttpMethod.POST));
    Validate.isTrue(respons.contains(HttpMethod.OPTIONS));

    return HttpResponse.from(HttpStatus.OK, Boolean.TRUE);
  }
}
