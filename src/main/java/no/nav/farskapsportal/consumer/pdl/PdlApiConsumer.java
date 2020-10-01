package no.nav.farskapsportal.consumer.pdl;

import static no.nav.farskapsportal.consumer.pdl.PdlApiConsumerEndpointName.PDL_API_GRAPHQL;
import static no.nav.farskapsportal.consumer.pdl.PdlDtoUtils.isMasterPdlOrFreg;
import static no.nav.farskapsportal.util.Utils.toSingletonOrThrow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.pdl.graphql.GraphQLRequest;
import no.nav.farskapsportal.consumer.pdl.graphql.GraphQLResponse;
import no.nav.farskapsportal.exception.UnrecoverableException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Builder
public class PdlApiConsumer {

  private static final String TEMA = "Tema";
  private static final String TEMA_FAR = "FAR";

  @NonNull private final RestTemplate restTemplate;
  @NonNull private final ConsumerEndpoint consumerEndpoint;

  public HttpResponse<Kjoenn> henteKjoenn(String foedselsnummer) {

    var respons = hentPersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_KJOENN);
    var kjoennDtos = respons.getData().getHentPerson().getKjoenn();

    var kjoennFraPdlEllerFreg =
        kjoennDtos.stream().filter(isMasterPdlOrFreg()).collect(Collectors.toList());

    if (kjoennFraPdlEllerFreg.isEmpty()) {
      return HttpResponse.from(HttpStatus.NOT_FOUND);
    }

    var kjoenn =
        kjoennFraPdlEllerFreg.stream()
            .filter(Objects::nonNull)
            .map(k -> Kjoenn.valueOf(k.getKjoenn().name()))
            .collect(
                toSingletonOrThrow(
                    new UnrecoverableException(
                        "Feil ved mapping av kjønn, forventet bare et registrert kjønn på person")));

    return HttpResponse.from(HttpStatus.OK, kjoenn);
  }

  @Retryable(maxAttempts = 10)
  private GraphQLResponse hentPersondokument(String ident, String query) {
    val graphQlRequest =
        GraphQLRequest.builder()
            .query(query)
            .variables(Map.of("historikk", false, "ident", ident))
            .build();

    var endpoint = consumerEndpoint.retrieveEndpoint(PDL_API_GRAPHQL);
    var response =
        restTemplate.postForEntity(endpoint, graphQlRequest, GraphQLResponse.class).getBody();

    log.info("Respons fra pdl-api: {}", response);

    return checkForPdlApiErrors(response);
  }

  private GraphQLResponse checkForPdlApiErrors(GraphQLResponse response) {
    Optional.ofNullable(response)
        .map(GraphQLResponse::getErrors)
        .ifPresent(
            errorJsonNodes -> {
              List<String> errors =
                  errorJsonNodes.stream()
                      .map(
                          jsonNode ->
                              jsonNode.get("message")
                                  + "(feilkode: "
                                  + jsonNode.path("extensions").path("code")
                                  + ")")
                      .collect(Collectors.toList());
              throw new PdlApiException(errors);
            });
    return response;
  }
}
