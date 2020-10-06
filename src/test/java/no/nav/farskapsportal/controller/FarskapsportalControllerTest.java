package no.nav.farskapsportal.controller;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@DisplayName("FarskapsportalController")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 8096)
public class FarskapsportalControllerTest {

  @LocalServerPort private int localServerPort;

  @Autowired private HttpHeaderTestRestTemplate httpHeaderTestRestTemplate;

  @Autowired private StsStub stsStub;

  @Autowired private PdlApiStub pdlApiStub;

  static <T> HttpEntity<T> initHttpEntity(T body, CustomHeader... customHeaders) {

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    if (customHeaders != null) {
      for (var header : customHeaders) {
        headers.add(header.headerName, header.headerValue);
      }
    }

    return new HttpEntity<>(body, headers);
  }

  @Test
  @DisplayName("Skal finne kjønn til person")
  void skalFinneKjoennTilPerson() {

    // given
    stsStub.runSecurityTokenServiceStub("jalla");
    pdlApiStub.runPdlApiHentPersonStub(List.of(new HentPersonKjoenn(Kjoenn.KVINNE)));

    // when
    var respons =
        httpHeaderTestRestTemplate.exchange(
            initHenteKjoennUrl(), HttpMethod.GET, null, String.class);

    // then
    assertAll(
        () -> assertThat(HttpStatus.OK.equals(respons.getStatusCode())),
        () -> assertThat(Kjoenn.KVINNE.name().equals(respons.getBody())));
  }

  @Test
  @DisplayName("Skal gi not found dersom person ikke eksisterer")
  void skalGiNotFoundDersomPersonIkkeEksisterer() {

    // given
    stsStub.runSecurityTokenServiceStub("jalla");
    pdlApiStub.runPdlApiHentPersonStub(List.of(new HentPersonKjoenn(Kjoenn.KVINNE)));

    // when
    var respons =
        httpHeaderTestRestTemplate.exchange(
            initHenteKjoennUrl(), HttpMethod.GET, null, String.class);

    // then
    assertAll(
        () -> assertThat(HttpStatus.OK.equals(respons.getStatusCode())),
        () -> assertThat(Kjoenn.KVINNE.name().equals(respons.getBody())));
  }

  @Test
  @DisplayName("Skal gi Ok dersom navn og kjønn er riktig")
  void skalGiOkDersomNavnOgKjoennErRiktig() {

    // given
    var registrertNavn = NavnDto.builder().fornavn("Borat").etternavn("Sagdiyev").build();
    stsStub.runSecurityTokenServiceStub("jalla");
    pdlApiStub.runPdlApiHentPersonStub(
        List.of(new HentPersonKjoenn(Kjoenn.MANN), new HentPersonNavn(registrertNavn)));

    // when
    var respons =
        httpHeaderTestRestTemplate.exchange(
            initKontrollereOpplysningerFar(),
            HttpMethod.POST,
            initHttpEntity(
                KontrollerePersonopplysningerRequest.builder()
                    .foedselsnummer("01057244444")
                    .fornavn("Borat")
                    .etternavn("Sagdiyev")
                    .build()),
            HttpStatus.class);

    // then
    assertTrue(respons.getStatusCode().is2xxSuccessful());
  }

  @Test
  @DisplayName("Skal gi bad request dersom oppgitt far er kvinne")
  void skalGiBadRequestDersomOppgittFarErKvinne() {

    // given
    var oppgittNavn = NavnDto.builder().fornavn("Natalya").etternavn("Sagdiyev").build();
    stsStub.runSecurityTokenServiceStub("jalla");
    pdlApiStub.runPdlApiHentPersonStub(
        List.of(new HentPersonKjoenn(Kjoenn.KVINNE), new HentPersonNavn(oppgittNavn)));

    // when
    var respons =
        httpHeaderTestRestTemplate.exchange(
            initKontrollereOpplysningerFar(),
            HttpMethod.POST,
            initHttpEntity(
                KontrollerePersonopplysningerRequest.builder()
                    .foedselsnummer("01058011444")
                    .fornavn("Natalya")
                    .etternavn("Sagdiyev")
                    .build()),
            String.class);

    // then
    assertTrue(respons.getStatusCode().is4xxClientError());
  }

  @Test
  @DisplayName("Skal gi bad request dersom navn er gjengitt feil i spørring")
  void skalGiBadRequestDersomNavnErGjengittFeilISpoerring() {

    // given
    var registrertNavn = NavnDto.builder().fornavn("Borat").etternavn("Sagdiyev").build();
    stsStub.runSecurityTokenServiceStub("jalla");
    pdlApiStub.runPdlApiHentPersonStub(
        List.of(new HentPersonKjoenn(Kjoenn.MANN), new HentPersonNavn(registrertNavn)));

    // when
    var respons =
        httpHeaderTestRestTemplate.exchange(
            initKontrollereOpplysningerFar(),
            HttpMethod.POST,
            initHttpEntity(
                KontrollerePersonopplysningerRequest.builder()
                    .foedselsnummer("01058011444")
                    .fornavn("Borat")
                    .mellomnavn("Nicolai")
                    .etternavn("Sagdiyev")
                    .build()),
            String.class);

    // then
    assertTrue(respons.getStatusCode().is4xxClientError());
  }

  @Test
  @DisplayName("Skal gi internal server error dersom person ikke eksisterer i PDL")
  void skalGiInternalServerErrorDersomPersonIkkeEksistererIPdl() {

    // given
    stsStub.runSecurityTokenServiceStub("jalla");
    pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();

    // when
    var respons =
        httpHeaderTestRestTemplate.exchange(
            initKontrollereOpplysningerFar(),
            HttpMethod.POST,
            initHttpEntity(
                KontrollerePersonopplysningerRequest.builder()
                    .foedselsnummer("01058011444")
                    .fornavn("Borat")
                    .etternavn("Sagdiyev")
                    .build()),
            String.class);

    // then
    assertTrue(respons.getStatusCode().is5xxServerError());
  }

  private String initHenteKjoennUrl() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/kjoenn";
  }

  private String initKontrollereOpplysningerFar() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/kontrollere/far";
  }

  private String getBaseUrlForStubs() {
    return "http://localhost:" + localServerPort;
  }

  private static class CustomHeader {

    String headerName;
    String headerValue;

    CustomHeader(String headerName, String headerValue) {
      this.headerName = headerName;
      this.headerValue = headerValue;
    }
  }
}
