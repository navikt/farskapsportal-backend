package no.nav.farskapsportal.backend.apps.asynkron.consumer.joark.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.dto.joark.api.OpprettJournalpostResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JournalpostApiStub {

  @Value("${url.joark.opprette-journalpost}")
  private String oppretteJournalpostEndpoint;

  public void runJournalpostApiStub(OpprettJournalpostResponse opprettJournalpostResponse) {

    stubFor(
        post(urlPathEqualTo(oppretteJournalpostEndpoint))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withStatus(201)
                    .withBody(
                        String.join(
                            "\n",
                            "{",
                            " \"journalpostId\": \"" + opprettJournalpostResponse.getJournalpostId() + "\",",
                            " \"journalpostferdigstilt\": \"" + opprettJournalpostResponse.getJournalpostferdigstilt() + "\",",
                            " \"journalStatus\": \"" + opprettJournalpostResponse.getJournalstatus() + "\",",
                            " \"melding\": \"" + opprettJournalpostResponse.getMelding() + "\",",
                            " \"dokumenter\": [",
                            " { ",
                            "   \"dokumentInfoId\":" + opprettJournalpostResponse.getDokumenter().get(0).getDokumentInfoId(),
                            "  }",
                            " ]",
                            "}")
                    )
            )
    );
  }
}
