package no.nav.farskapsportal.consumer.pdl;

import static java.util.stream.Collectors.toList;
import static no.nav.farskapsportal.consumer.pdl.PdlApiConsumerEndpointName.PDL_API_GRAPHQL;
import static no.nav.farskapsportal.consumer.pdl.PdlDtoUtils.isMasterPdlOrFreg;
import static no.nav.farskapsportal.util.Utils.toSingletonOrThrow;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonerDto;
import no.nav.farskapsportal.consumer.pdl.api.FoedselDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennDto;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.graphql.GraphQLRequest;
import no.nav.farskapsportal.consumer.pdl.graphql.GraphQLResponse;
import no.nav.farskapsportal.exception.UnrecoverableException;
import org.apache.commons.lang3.Validate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Builder
public class PdlApiConsumer {

  private static final String TEMA = "Tema";
  private static final String TEMA_FAR = "FAR";

  @NonNull private final RestTemplate restTemplate;
  @NonNull private final ConsumerEndpoint consumerEndpoint;

  public LocalDate henteFoedselsdato(String foedselsnummer) {
    var respons = hentPersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_FOEDSEL, false);
    var foedselDtos = respons.getData().getHentPerson().getFoedsel();

    var foedselDtosFraPdlEllerFreg =
        foedselDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (foedselDtosFraPdlEllerFreg.isEmpty()) {
      throw new PersonIkkeFunnetException(
          "Respons fra PDL inneholdt ingen informasjon om personens foedselsdato...");
    }

    return foedselDtos.stream()
        .filter(Objects::nonNull)
        .map(FoedselDto::getFoedselsdato)
        .findFirst()
        .orElseThrow(() -> new PdlApiException("Feil oppstod ved henting av fødselsdato for person"));
  }

  public List<FamilierelasjonerDto> henteFamilierelasjoner(String foedselsnummer) {
    var respons =
        hentPersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_FAMILIERELASJONER, false);
    var familierelasjonerDtos = respons.getData().getHentPerson().getFamilierelasjoner();
    var familierelasjonerFraPdlEllerFreg =
        familierelasjonerDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    return familierelasjonerFraPdlEllerFreg;
  }

  public KjoennDto henteKjoennUtenHistorikk(String foedselsnummer) {

    var kjoennFraPdlEllerFreg = henteKjoenn(foedselsnummer, false);

    return kjoennFraPdlEllerFreg.stream()
        .filter(Objects::nonNull)
        .collect(
            toSingletonOrThrow(
                new UnrecoverableException(
                    "Feil ved mapping av kjønn, forventet bare et registrert kjønn på person")));
  }

  public List<no.nav.farskapsportal.consumer.pdl.api.KjoennDto> henteKjoennMedHistorikk(String foedselsnummer) {
    var kjoennshistorikk = henteKjoenn(foedselsnummer, true);

    return kjoennshistorikk.stream()
        .filter(Objects::nonNull)
        .collect(toList());
  }

  private List<no.nav.farskapsportal.consumer.pdl.api.KjoennDto> henteKjoenn(String foedselsnummer, boolean inkludereHistorikk) {
    var respons =
        hentPersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_KJOENN, inkludereHistorikk);
    var kjoennDtos = respons.getData().getHentPerson().getKjoenn();
    var kjoennFraPdlEllerFreg = kjoennDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (kjoennFraPdlEllerFreg.isEmpty()) {
      throw new PersonIkkeFunnetException(
          "Respons fra PDL inneholdt ingen informasjon om kjønn...");
    }

    return kjoennFraPdlEllerFreg;
  }

  @NotNull
  public NavnDto hentNavnTilPerson(String foedselsnummer) {
    var respons = hentPersondokument(foedselsnummer, PdlApiQuery.HENT_PERSON_NAVN, false);
    var navnDtos = respons.getData().getHentPerson().getNavn();

    var navnFraPdlEllerFreg = navnDtos.stream().filter(isMasterPdlOrFreg()).collect(toList());

    if (navnFraPdlEllerFreg.isEmpty()) {
      throw new PersonIkkeFunnetException("Fant ikke personens navn i PDL");
    }

    var navnDto =
        navnFraPdlEllerFreg.stream()
            .filter(Objects::nonNull)
            .collect(
                toSingletonOrThrow(
                    new UnrecoverableException(
                        "Feil ved mapping av kjønn, forventet bare et registrert kjønn på person")));

    Validate.notNull(navnDto.getFornavn(), "Fornavn mangler i retur fra PDL!");
    Validate.notNull(navnDto.getEtternavn(), "Etternavn mangler i retur fra PDL!");

    return navnDto;
  }

  @Retryable(maxAttempts = 10)
  private GraphQLResponse hentPersondokument(
      String ident, String query, boolean inkludereHistorikk) {
    val graphQlRequest =
        GraphQLRequest.builder()
            .query(query)
            .variables(Map.of("historikk", inkludereHistorikk, "ident", ident))
            .build();

    var endpoint = consumerEndpoint.retrieveEndpoint(PDL_API_GRAPHQL);
    GraphQLResponse response = null;
    try {
      response =
          restTemplate.postForEntity(endpoint, graphQlRequest, GraphQLResponse.class).getBody();
    } catch (Exception e) {
      e.printStackTrace();
    }

    log.info("Respons fra pdl-api: {}", response);

    return checkForPdlApiErrors(response);
  }

  private GraphQLResponse checkForPdlApiErrors(GraphQLResponse response) {
    Optional.ofNullable(response)
        .map(GraphQLResponse::getErrors)
        .ifPresent(
            errorJsonNodes -> {
              List<PdlApiError> errors =
                  errorJsonNodes.stream()
                      .map(
                          jsonNode ->
                              PdlApiError.builder()
                                  .message(jsonNode.get("message").toString())
                                  .code(jsonNode.path("extensions").path("code").toString())
                                  .build())
                      .collect(toList());

              for (PdlApiError error : errors) {
                if (error.getMessage().contains("Fant ikke person")
                    && error.getCode().contains("not_found")) {
                  throw new PersonIkkeFunnetException("Fant ikke person i PDL");
                }
              }
              throw new PdlApiErrorException(errors);
            });
    return response;
  }
}
