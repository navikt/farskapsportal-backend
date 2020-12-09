package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.lageUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppretteFarskaperklaeringRequest;
import no.nav.farskapsportal.config.FarskapsportalConfig.OidcTokenSubjectExtractor;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.KjoennTypeDto;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.DokumentStatusDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.SignaturDto;
import no.nav.farskapsportal.service.PersistenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

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

  @MockBean private OidcTokenSubjectExtractor oidcTokenSubjectExtractor;

  @MockBean private PdfGeneratorConsumer pdfGeneratorConsumer;

  @MockBean private DifiESignaturConsumer difiESignaturConsumer;

  @MockBean private PersistenceService persistenceService;

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

  private String initHenteKjoennUrl() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/kjoenn";
  }

  private String initKontrollereOpplysningerFar() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/personopplysninger/far";
  }

  private String initNyFarskapserklaering() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/farskapserklaering/ny";
  }

  private String initHenteDokumentEtterRedirect() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/farskapserklaering/redirect";
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

  @Nested
  @DisplayName("Teste henteBrukerinformasjon")
  class HenteBrukerinformasjon {

    @Test
    @DisplayName("Skal finne kjønn til person")
    void skalFinneKjoennTilPerson() {

      // given
      stsStub.runSecurityTokenServiceStub("jalla");
      pdlApiStub.runPdlApiHentPersonStub(List.of(new HentPersonKjoenn(KjoennTypeDto.KVINNE)));

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
      pdlApiStub.runPdlApiHentPersonStub(List.of(new HentPersonKjoenn(KjoennTypeDto.KVINNE)));

      // when
      var respons =
          httpHeaderTestRestTemplate.exchange(
              initHenteKjoennUrl(), HttpMethod.GET, null, String.class);

      // then
      assertAll(
          () -> assertThat(HttpStatus.OK.equals(respons.getStatusCode())),
          () -> assertThat(Kjoenn.KVINNE.name().equals(respons.getBody())));
    }
  }

  @Nested
  @DisplayName("Teste kontrollereOpplysningerFar")
  class KontrollereOpplysningerFar {

    @Test
    @DisplayName("Skal gi Ok dersom navn og kjønn er riktig")
    void skalGiOkDersomNavnOgKjoennErRiktig() {

      // given
      var registrertNavn = NavnDto.builder().fornavn("Borat").etternavn("Sagdiyev").build();
      stsStub.runSecurityTokenServiceStub("jalla");

      var kjoennshistorikk =
          Stream.of(new Object[][] {{KjoennTypeDto.MANN, LocalDateTime.now()}})
              .collect(
                  Collectors.toMap(
                      data -> (KjoennTypeDto) data[0], data -> (LocalDateTime) data[1]));

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikk), new HentPersonNavn(registrertNavn)));

      // when
      var respons =
          httpHeaderTestRestTemplate.exchange(
              initKontrollereOpplysningerFar(),
              HttpMethod.POST,
              initHttpEntity(
                  KontrollerePersonopplysningerRequest.builder()
                      .foedselsnummer("01057244444")
                      .navn("Borat Sagdiyev")
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

      var kjoennshistorikk =
          Stream.of(new Object[][] {{KjoennTypeDto.KVINNE, LocalDateTime.now()}})
              .collect(
                  Collectors.toMap(
                      data -> (KjoennTypeDto) data[0], data -> (LocalDateTime) data[1]));

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikk), new HentPersonNavn(oppgittNavn)));

      // when
      var respons =
          httpHeaderTestRestTemplate.exchange(
              initKontrollereOpplysningerFar(),
              HttpMethod.POST,
              initHttpEntity(
                  KontrollerePersonopplysningerRequest.builder()
                      .foedselsnummer("01058011444")
                      .navn("Natalya Sagdiyev")
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

      var kjoennshistorikk =
          Stream.of(new Object[][] {{KjoennTypeDto.MANN, LocalDateTime.now()}})
              .collect(
                  Collectors.toMap(
                      data -> (KjoennTypeDto) data[0], data -> (LocalDateTime) data[1]));

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikk), new HentPersonNavn(registrertNavn)));

      // when
      var respons =
          httpHeaderTestRestTemplate.exchange(
              initKontrollereOpplysningerFar(),
              HttpMethod.POST,
              initHttpEntity(
                  KontrollerePersonopplysningerRequest.builder()
                      .foedselsnummer("01058011444")
                      .navn("Borat Nicolai Sagdiyev")
                      .build()),
              String.class);

      // then
      assertTrue(respons.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Skal gi not found dersom person ikke eksisterer i PDL")
    void skalGiNotFoundDersomPersonIkkeEksistererIPdl() {

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
                      .navn("Borat Sagdiyev")
                      .build()),
              String.class);

      // then
      assertSame(respons.getStatusCode(), HttpStatus.NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("Teste nyFarskapserklaering")
  class NyFarskapserklaering {

    @Test
    @DisplayName("Skal opprette farskapserklaering for barn med termindato")
    void skalOppretteFarskapserklaeringForBarnMedTermindato() {

      // given
      var fnrMor = "11111112345";
      var registrertNavnMor = NavnDto.builder().fornavn("Natalya").etternavn("Sagdiyev").build();
      var registrertNavnFar = NavnDto.builder().fornavn("Jessie").etternavn("James").build();
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer("00000012121")
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      stsStub.runSecurityTokenServiceStub("jalla");

      var kjoennshistorikkMor =
          Stream.of(new Object[][] {{KjoennTypeDto.KVINNE, LocalDateTime.now()}})
              .collect(
                  Collectors.toMap(
                      data -> (KjoennTypeDto) data[0], data -> (LocalDateTime) data[1]));

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkMor), new HentPersonNavn(registrertNavnMor)),
          fnrMor);

      var kjoennshistorikkFar =
          Stream.of(new Object[][] {{KjoennTypeDto.MANN, LocalDateTime.now()}})
              .collect(
                  Collectors.toMap(
                      data -> (KjoennTypeDto) data[0], data -> (LocalDateTime) data[1]));

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar), new HentPersonNavn(registrertNavnFar)),
          opplysningerOmFar.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(fnrMor);
      var redirectUrlMor =
          URI.create(
              "https://redirect.mot.signeringstjensesten.settes.under.normal.kjoering.etter.opprettelse.av.signeringsjobb.no");

      var pdf =
          DokumentDto.builder()
              .dokumentnavn("Farskapserklæering.pdf")
              .innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
              .redirectUrlMor(redirectUrlMor)
              .build();

      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);
      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());
      when(persistenceService.lagreFarskapserklaering(any())).thenReturn(null);

      // when
      var respons =
          httpHeaderTestRestTemplate.exchange(
              initNyFarskapserklaering(),
              HttpMethod.POST,
              initHttpEntity(
                  OppretteFarskaperklaeringRequest.builder()
                      .barn(BarnDto.builder().termindato(LocalDate.now().plusMonths(3)).build())
                      .opplysningerOmFar(opplysningerOmFar)
                      .build()),
              URI.class);

      // then
      assertTrue(respons.getStatusCode().is2xxSuccessful());
      assertEquals(redirectUrlMor, respons.getBody());
    }
  }

  @Nested
  @DisplayName("Teste henteDokumentEtterRedirect")
  class HenteDokumentEtterRedirect {

    @SneakyThrows
    @Test
    @DisplayName("Skal hente signert dokument for far etter redirect")
    void skalHenteSignertDokumentForFarEtterRedirect() {

      // given
      var registrertNavnFar = NavnDto.builder().fornavn("Jessie").etternavn("James").build();
      var far =
          ForelderDto.builder()
              .foedselsnummer("00001122111")
              .fornavn(registrertNavnFar.getFornavn())
              .etternavn(registrertNavnFar.getEtternavn())
              .build();

      var mor =
          ForelderDto.builder()
              .foedselsnummer("11001122110")
              .fornavn("Dolly")
              .etternavn("Duck")
              .build();

      var statuslenke = "https://hvaskjera.no/";
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(far.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");
      var kjoennshistorikkFar =
          Stream.of(new Object[][] {{KjoennTypeDto.MANN, LocalDateTime.now()}})
              .collect(
                  Collectors.toMap(
                      data -> (KjoennTypeDto) data[0], data -> (LocalDateTime) data[1]));

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar), new HentPersonNavn(registrertNavnFar)),
          far.getFoedselsnummer());

      var farskapserklaering =
          FarskapserklaeringDto.builder()
              .mor(mor)
              .far(far)
              .dokument(
                  DokumentDto.builder()
                      .redirectUrlFar(lageUrl("redirect-far"))
                      .innhold("Jeg erklærer herved farskap til dette barnet..".getBytes())
                      .dokumentnavn("farskapserklæring.pdf")
                      .dokumentStatusUrl(new URI(statuslenke))
                      .build())
              .build();

      when(persistenceService.henteFarskapserklaeringerEtterRedirect(
              far.getFoedselsnummer(), Forelderrolle.FAR, KjoennTypeDto.MANN))
          .thenReturn(Set.of(farskapserklaering));

      when(persistenceService.lagreFarskapserklaering(any())).thenReturn(null);

      when(difiESignaturConsumer.henteDokumentstatusEtterRedirect(any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .statuslenke(new URI(statuslenke))
                  .erSigneringsjobbenFerdig(true)
                  .padeslenke(new URI("https://permanent-pades-url.no/"))
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(far.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForSignering(LocalDateTime.now().minusSeconds(3))
                              .build()))
                  .build());

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(farskapserklaering.getDokument().getInnhold());

      // when
      var respons =
          httpHeaderTestRestTemplate.exchange(
              UriComponentsBuilder.fromHttpUrl(initHenteDokumentEtterRedirect())
                  .queryParam("status_query_token", "Sjalalala-lala")
                  .build()
                  .encode()
                  .toString(),
              HttpMethod.PUT,
              initHttpEntity("innhold til signert dokument".getBytes()),
              byte[].class);

      // then
      assertTrue(respons.getStatusCode().is2xxSuccessful());
    }
  }
}
