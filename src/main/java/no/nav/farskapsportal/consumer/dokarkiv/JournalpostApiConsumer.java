package no.nav.farskapsportal.consumer.dokarkiv;


import static no.nav.farskapsportal.consumer.dokarkiv.JournalpostApiConsumerEndpointName.ARKIVERE_JOURNALPOST;

import java.util.Optional;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.dokarkiv.api.OpprettJournalpostResponse;
import no.nav.farskapsportal.consumer.dokarkiv.mapping.FarskapsportalJoarkMapper;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

public class JournalpostApiConsumer {

  private final RestTemplate restTemplate;
  private final ConsumerEndpoint journalpostapiEndpoints;
  private final FarskapsportalJoarkMapper farskapsportalTilJoarkMapper;

  public JournalpostApiConsumer(RestTemplate restTemplate, ConsumerEndpoint journalpostapiEndpoints, FarskapsportalJoarkMapper farskapsportalTilJoarkMapper) {
    this.restTemplate = restTemplate;
    this.journalpostapiEndpoints = journalpostapiEndpoints;
    this.farskapsportalTilJoarkMapper = farskapsportalTilJoarkMapper;
  }

  public HttpResponse<OpprettJournalpostResponse> arkivereFarskapserklaering(Farskapserklaering farskapserklaering) {
    var endpoint = journalpostapiEndpoints.retrieveEndpoint(ARKIVERE_JOURNALPOST);
    var request = farskapsportalTilJoarkMapper.tilJoark(farskapserklaering);
    var responseEntity = restTemplate.postForEntity(endpoint, new HttpEntity<>(request), OpprettJournalpostResponse.class);

    return Optional.ofNullable(responseEntity).map(response -> HttpResponse
        .from(response.getStatusCode(), response.getBody()))
        .orElseGet(() -> HttpResponse.from(HttpStatus.INTERNAL_SERVER_ERROR));
  }
}
