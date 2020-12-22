package no.nav.farskapsportal.consumer.esignering.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DifiESignaturStub {

  public void runOppretteSigneringsjobbStub(String statusUrl, String redirectUrlMor, String redirectUrlFar) {
    stubFor(
        post(urlPathMatching("/esignering.*"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                    .withStatus(200)
                    .withBody(
                        String.join(
                            "\n",
                            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
                            " <direct-signature-job-response xmlns=\"http://signering.posten.no/schema/v1\">",
                            "   <signature-job-id>1</signature-job-id>",
                            "     <redirectUrl>https://redirect-ikke-i-bruk.posten.no/</redirectUrl>",
                            "     <status-url>" + statusUrl + "</status-url>",
                            "     <signer href=\"https://api.signering.posten.no/api/12345678910/direct/signature-jobs/1/signers/1\">",
                            "       <personal-identification-number>12345678910</personal-identification-number>",
                            "       <redirect-url>" + redirectUrlMor + "</redirect-url>",
                            "     </signer>",
                            "     <signer href=\"https://api.signering.posten.no/api/11111122222/direct/signature-jobs/1/signers/2\">",
                            "       <personal-identification-number>11111122222</personal-identification-number>",
                            "       <redirect-url>" + redirectUrlFar + "</redirect-url>",
                            "     </signer>",
                            " </direct-signature-job-response>"))));
  }

  public void runGetStatus(String statusUrl, String padesUrl, String idSender) {
    stubFor(
        get(urlPathMatching("/api/12345678910/direct/signature-jobs/1/status.*"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                    .withStatus(200)
                    .withBody(
                        String.join(
                            "\n",
                            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
                            " <direct-signature-job-status-response xmlns=\"http://signering.posten.no/schema/v1\">",
                            "   <signature-job-id>1</signature-job-id>",
                            "   <signature-job-status>COMPLETED_SUCCESSFULLY</signature-job-status>",
                            "   <status-url>" + statusUrl + "</status-url>",
                            "   <status signer=\"" + idSender + "\" since=\"" + LocalDateTime.now().minusSeconds(10) + "\">SIGNED</status>",
                            "   <confirmation-url>https://api.signering.posten.no/api/" + idSender + "/direct/signature-jobs/1/complete</confirmation-url>",
                            "   <xades-url signer=\"" + idSender + "\">https://api.signering.posten.no/api/" + idSender + "/direct/signature-jobs/1/xades/1</xades-url>",
                            "   <pades-url>" + padesUrl + "</pades-url>",
                            " </direct-signature-job-status-response>"))));
  }

  public void runGetSignedDocuments(String statusUrl, String padesUrl, String idSender) {
    stubFor(
        get(urlPathMatching("/api/12345678910/direct/signature-jobs/1/status.*"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                    .withStatus(200)
                    .withBody(
                        String.join(
                            "\n",
                            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
                            " <direct-signer-response xmlns=\"http://signering.posten.no/schema/v1\">",
                            "   <personal-identification-number>" + idSender + "</personal-identification-number>",
                            "   <confirmation-url>https://api.signering.posten.no/api/" + idSender + "/direct/signature-jobs/1/complete</confirmation-url>",
                            "   <xades-url signer=\"" + idSender + "\">https://api.signering.posten.no/api/" + idSender + "/direct/signature-jobs/1/xades/1</xades-url>",
                            "   <pades-url>" + padesUrl + "</pades-url>",
                            " </direct-signature-job-status-response>"))));
  }
}
