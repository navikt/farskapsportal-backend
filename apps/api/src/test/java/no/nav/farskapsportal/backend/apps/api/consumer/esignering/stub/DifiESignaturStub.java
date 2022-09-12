package no.nav.farskapsportal.backend.apps.api.consumer.esignering.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DifiESignaturStub {

  public final static String PATH_SIGNER_URL_MOR = "/api/12345678910/direct/signature-jobs/1/signers/1";
  public final static String PATH_SIGNER_URL_FAR = "/api/11111122222/direct/signature-jobs/1/signers/1";

  @Value("${wiremock.server.port}")
  String wiremockPort;

  public void runOppretteSigneringsjobbStub(String statusUrl, String redirectUrlMor, String redirectUrlFar) {

    var baseWireMockUrl = "http://localhost:" + wiremockPort;

    stubFor(
        post(urlPathMatching("/esignering.*"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                    .withStatus(200)
                    .withBody(mockDirectSignatureJobResponse(statusUrl, baseWireMockUrl + PATH_SIGNER_URL_MOR, baseWireMockUrl + PATH_SIGNER_URL_FAR,
                        redirectUrlMor, redirectUrlFar)))
    );
  }

  public void runGetStatus(String statusUrl, String padesUrl, String idSigner1, String idSigner2) {
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
                            "   <status signer=\"" + idSigner1 + "\" since=\"" + Instant.now().minusSeconds(10) + "\">SIGNED</status>",
                            "   <status signer=\"" + idSigner2 + "\" since=\"" + Instant.now().minusSeconds(10) + "\">SIGNED</status>",
                            "   <confirmation-url>https://api.signering.posten.no/api/" + idSigner1
                                + "/direct/signature-jobs/1/complete</confirmation-url>",
                            "   <xades-url signer=\"" + idSigner1 + "\">https://api.signering.posten.no/api/" + idSigner1
                                + "/direct/signature-jobs/1/xades/1</xades-url>",
                            "   <xades-url signer=\"" + idSigner2 + "\">https://api.signering.posten.no/api/" + idSigner2
                                + "/direct/signature-jobs/1/xades/1</xades-url>",
                            "   <pades-url>" + padesUrl + "</pades-url>",
                            " </direct-signature-job-status-response>"))));
  }

  public void runGetNyRedirecturl(String fnr, String signerUrl, String redirectUrl) {
    stubFor(
        post(urlPathMatching(".*signers/.*"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                    .withStatus(200)
                    .withBody(mockDirectSignerResponse(fnr, signerUrl, redirectUrl)))
    );
  }

  public void runGetSignedDocument(String padesPath) {
    stubFor(
        get(urlPathMatching(".*" + padesPath))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .withStatus(200)
                    .withBodyFile("Farskapserklæring.pdf")));
  }

  public void runGetXades(String xadesPath) {
    stubFor(
        get(urlPathMatching(".*" + xadesPath))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .withStatus(200)
                    .withBody("Jeg har signert".getBytes(StandardCharsets.UTF_8))));

  }

  private String mockDirectSignatureJobResponse(String statusUrl, String signerUrlMor, String signerUrlFar, String redirectUrlMor,
      String redirectUrlFar) {
    return String.join(
        "\n",
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
        " <direct-signature-job-response xmlns=\"http://signering.posten.no/schema/v1\">",
        "   <signature-job-id>1</signature-job-id>",
        "     <redirectUrl>https://redirect-ikke-i-bruk.posten.no/</redirectUrl>",
        "     <status-url>" + statusUrl + "</status-url>",
        "     <signer href=\"" + signerUrlMor + "\">",
        "       <personal-identification-number>12345678910</personal-identification-number>",
        "       <redirect-url>" + redirectUrlMor + "</redirect-url>",
        "     </signer>",
        "     <signer href=\"" + signerUrlFar + "\">",
        "       <personal-identification-number>11111122222</personal-identification-number>",
        "       <redirect-url>" + redirectUrlFar + "</redirect-url>",
        "     </signer>",
        " </direct-signature-job-response>");
  }

  private String mockDirectSignerResponse(String fnr, String signerUrl, String redirectUrl) {
    return String.join(
        "\n",
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
        "<direct-signer-response xmlns=\"http://signering.posten.no/schema/v1\" href=\"" + signerUrl + "\">",
        "    <personal-identification-number>" + fnr + "</personal-identification-number>",
        "    <redirect-url>" + redirectUrl + "</redirect-url>",
        "</direct-signer-response>"
    );
  }
}
