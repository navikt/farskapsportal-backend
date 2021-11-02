package no.nav.farskapsportal.backend.apps.asynkron.consumer.joark;


import static no.nav.farskapsportal.backend.apps.asynkron.consumer.joark.JournalpostApiConsumerEndpointName.ARKIVERE_JOURNALPOST;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.exception.JournalpostApiConsumerException;
import no.nav.farskapsportal.backend.libs.dto.joark.api.OpprettJournalpostResponse;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class JournalpostApiConsumer {

  private final RestTemplate restTemplate;
  private final ConsumerEndpoint journalpostapiEndpoints;
  private final FarskapsportalJoarkMapper farskapsportalTilJoarkMapper;

  public JournalpostApiConsumer(RestTemplate restTemplate,
      ConsumerEndpoint journalpostapiEndpoints,
      FarskapsportalJoarkMapper farskapsportalJoarkMapper) {
    this.restTemplate = restTemplate;
    this.journalpostapiEndpoints = journalpostapiEndpoints;
    this.farskapsportalTilJoarkMapper = farskapsportalJoarkMapper;
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
