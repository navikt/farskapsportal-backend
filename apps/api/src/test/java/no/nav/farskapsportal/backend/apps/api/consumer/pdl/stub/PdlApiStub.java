package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.dto.pdl.EndringDto.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PdlApiStub {

  @Value("${url.pdl-api.graphql}")
  private String pdlApiGraphqlEndpoint;

  private static String stubHentPerson(List<HentPersonSubResponse> subResponses, boolean medHistorikk) {

    var startingElements = String.join("\n", " {", " \"data\": {", " \"hentPerson\": {");

    var closingElements = String.join("\n", "}", "}", "}");

    var stubResponse = new StringBuilder();
    stubResponse.append(startingElements);

    var count = 0;
    for (HentPersonSubResponse subResponse : subResponses) {
      stubResponse.append(subResponse.hentRespons(medHistorikk));
      if (subResponses.size() > 1 && (count == 0 || count < (subResponses.size() - 1))) {
        stubResponse.append(",");
      }
      count++;
    }

    stubResponse.append(closingElements);

    return stubResponse.toString();
  }

  public static String hentFolkerigstermetadataElement(LocalDateTime gyldighetstidspunkt) {
    return String.join(
        "\n",
        " \"folkeregistermetadata\": {",
        "   \"gyldighetstidspunkt\": \"" + gyldighetstidspunkt + "\"",
        " }");
  }

  public static String hentMetadataElement(String opplysningsId, boolean historisk, LocalDateTime opprettet) {
    return String.join(
        "\n",
        " \"metadata\": {",
        "   \"historisk\": \"" + historisk + "\",",
        "   \"opplysningsId\": \"" + opplysningsId + "\",",
        "   \"master\": \"Freg\",",
        "   \"endringer\": [",
        " {",
        "         \"type\": \"" + Type.OPPRETT + "\",",
        "         \"registrert\": \"" + opprettet.toString() + "\",",
        "         \"registrertAv\": \"Folkeregisteret\",",
        "         \"systemkilde\": \"FREG\"",
        "     }",
        " ]",
        " }");
  }

  public void runPdlApiHentPersonStub(List<HentPersonSubResponse> subResponses) {
    runPdlApiHentPersonStub(subResponses, "");
  }

  public void runPdlApiHentPersonStub(List<HentPersonSubResponse> subResponses, String ident) {

    var responsUtenHistorikk = stubHentPerson(subResponses, false);
    var responsMedHistorikk = stubHentPerson(subResponses, true);

    stubFor(post(urlEqualTo(pdlApiGraphqlEndpoint)).withRequestBody(containing(ident)).withRequestBody(containing("historikk\":false")).willReturn(
        aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withStatus(HttpStatus.OK.value()).withBody(responsUtenHistorikk)));

    stubFor(post(urlEqualTo(pdlApiGraphqlEndpoint)).withRequestBody(containing(ident)).withRequestBody(containing("historikk\":true")).willReturn(
        aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withStatus(HttpStatus.OK.value()).withBody(responsMedHistorikk)));
  }

  public void runPdlApiHentPersonFantIkkePersonenStub() {
    stubFor(post(urlEqualTo(pdlApiGraphqlEndpoint)).willReturn(
        aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withStatus(HttpStatus.OK.value()).withBody(String
            .join("\n", " {", "\"errors\": [", "{", "\"message\": \"Fant ikke person\",", "\"locations\": [", "{", "\"line\": 8,", "\"column\": 3",
                "}", "],", "\"path\": [", "\"hentPerson\"", "],", "\"extensions\": {", "\"code\": \"not_found\",",
                "\"classification\": \"ExecutionAborted\"", "}", "}", "],", "\"data\": {", "\"hentPerson\": null", "}", "}"))));
  }

  public void runPdlApiHentPersonValideringsfeil() {
    stubFor(post(urlEqualTo(pdlApiGraphqlEndpoint)).willReturn(
        aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withStatus(HttpStatus.OK.value()).withBody(String
            .join("\n", " {", "\"errors\": [", "{",
                "\"message\": \"Validation error of type FieldUndefined: Field 'mellomnav' in type 'Navn' is undefined @ 'hentPerson/navn/mellomnav\",",
                "\"locations\": [", "{", "\"line\": 11,", "\"column\": 5", "}", "],", "\"extensions\": {", "\"classification\": \"ValidationError\"",
                "}", "}", "]", "}"))));
  }
}
