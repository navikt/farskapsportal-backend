package no.nav.farskapsportal.consumer.pdl.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.spec.internal.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PdlApiStub {

  @Value("${url.pdl-api.graphql}")
  private String pdlApiGraphqlEndpoint;

  private static String stubHentPerson(List<HentPersonSubResponse> subResponses) {

    var startingElements = String.join("\n", " {", " \"data\": {", " \"hentPerson\": {");

    var closingElements = String.join("\n", "}", "}", "}");

    var stubResponse = new StringBuilder();
    stubResponse.append(startingElements);

    var count = 0;
    for (HentPersonSubResponse subResponse : subResponses) {
      stubResponse.append(subResponse.getResponse());
      if (subResponses.size() > 1 && (count == 0 || count < (subResponses.size() - 1))) {
        stubResponse.append(",");
      }
      count++;
    }

    stubResponse.append(closingElements);

    return stubResponse.toString();
  }

  public void runPdlApiHentPersonStub(List<HentPersonSubResponse> subResponses) {
    runPdlApiHentPersonStub(subResponses, "");
  }

  public void runPdlApiHentPersonStub(List<HentPersonSubResponse> subResponses, String ident) {

    var q = stubHentPerson(subResponses);

    stubFor(post(urlEqualTo(pdlApiGraphqlEndpoint)).withRequestBody(containing(ident)).willReturn(
        aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withStatus(HttpStatus.OK).withBody(stubHentPerson(subResponses))));
  }

  public void runPdlApiHentPersonFantIkkePersonenStub() {
    stubFor(post(urlEqualTo(pdlApiGraphqlEndpoint)).willReturn(
        aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withStatus(HttpStatus.OK).withBody(String
            .join("\n", " {", "\"errors\": [", "{", "\"message\": \"Fant ikke person\",", "\"locations\": [", "{", "\"line\": 8,", "\"column\": 3",
                "}", "],", "\"path\": [", "\"hentPerson\"", "],", "\"extensions\": {", "\"code\": \"not_found\",",
                "\"classification\": \"ExecutionAborted\"", "}", "}", "],", "\"data\": {", "\"hentPerson\": null", "}", "}"))));
  }

  public void runPdlApiHentPersonValideringsfeil() {
    stubFor(post(urlEqualTo(pdlApiGraphqlEndpoint)).willReturn(
        aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).withStatus(HttpStatus.OK).withBody(String
            .join("\n", " {", "\"errors\": [", "{",
                "\"message\": \"Validation error of type FieldUndefined: Field 'mellomnav' in type 'Navn' is undefined @ 'hentPerson/navn/mellomnav\",",
                "\"locations\": [", "{", "\"line\": 11,", "\"column\": 5", "}", "],", "\"extensions\": {", "\"classification\": \"ValidationError\"",
                "}", "}", "]", "}"))));
  }

  public static String hentFolkerigstermetadataElement(LocalDateTime gyldighetstidspunkt) {
    return String.join(
        "\n",
        " \"folkeregistermetadata\": {",
        "   \"gyldighetstidspunkt\": \"" + gyldighetstidspunkt + "\"",
        " }");
  }

  public static String hentMetadataElement(String opplysningsId, boolean historisk) {
    return String.join(
        "\n",
        " \"metadata\": {",
        "   \"historisk\": \"" + historisk + "\",",
        "   \"opplysningsId\": \"" + opplysningsId + "\",",
        "   \"master\": \"Freg\"",
        " }");
  }
}
