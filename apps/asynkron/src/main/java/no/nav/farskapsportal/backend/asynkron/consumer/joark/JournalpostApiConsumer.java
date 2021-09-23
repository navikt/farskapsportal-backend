package no.nav.farskapsportal.backend.asynkron.consumer.joark;


import static no.nav.farskapsportal.consumer.joark.JournalpostApiConsumerEndpointName.ARKIVERE_JOURNALPOST;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.joark.api.OpprettJournalpostResponse;
import no.nav.farskapsportal.exception.JournalpostApiConsumerException;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class JournalpostApiConsumer {

  private final RestTemplate restTemplate;
  private final ConsumerEndpoint journalpostapiEndpoints;
  private final FarskapsportalJoarkMapper farskapsportalTilJoarkMapper;

  public JournalpostApiConsumer(RestTemplate restTemplate,
      ConsumerEndpoint journalpostapiEndpoints,
      FarskapsportalJoarkMapper farskapsportalTilJoarkMapper) {
    this.restTemplate = restTemplate;
    this.journalpostapiEndpoints = journalpostapiEndpoints;
    this.farskapsportalTilJoarkMapper = farskapsportalTilJoarkMapper;
  }

  public OpprettJournalpostResponse arkivereFarskapserklaering(Farskapserklaering farskapserklaering) {
    var endpoint = journalpostapiEndpoints.retrieveEndpoint(ARKIVERE_JOURNALPOST);
    var request = farskapsportalTilJoarkMapper.tilJoark(farskapserklaering);

    try {
      var responseEntity = restTemplate.postForEntity(endpoint, new HttpEntity<>(request), OpprettJournalpostResponse.class);
      return Optional.ofNullable(responseEntity).filter(r -> r.getStatusCode().is2xxSuccessful()).map(r -> r.getBody())
          .orElseThrow(() -> new JournalpostApiConsumerException(Feilkode.JOARK_OVERFOERING_FEILET));
    } catch (Exception e) {
      e.printStackTrace();
      throw new JournalpostApiConsumerException(Feilkode.JOARK_OVERFOERING_FEILET, e);
    }
  }
}
