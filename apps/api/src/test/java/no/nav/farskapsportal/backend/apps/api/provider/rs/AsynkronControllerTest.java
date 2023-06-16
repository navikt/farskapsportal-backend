package no.nav.farskapsportal.backend.apps.api.provider.rs;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.farskapsportal.backend.apps.api.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.dto.asynkroncontroller.HenteAktoeridRequest;
import no.nav.farskapsportal.backend.libs.felles.exception.EsigneringStatusFeiletException;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApiApplicationLocal.class)
public class AsynkronControllerTest {

  @LocalServerPort private int localServerPort;

  @Autowired
  @Qualifier("asynkron")
  private HttpHeaderTestRestTemplate httpHeaderTestRestTemplateAsynkron;

  @MockBean private FarskapsportalService farskapsportalService;

  @MockBean private PersonopplysningService personopplysningService;

  @Nested
  @DisplayName("Tester for endepunkt synkronisereSigneringsstatusForFarIFarskapserklaering")
  class SynkronisereStatus {

    @Test
    void skalGiAcceptedDersomFarskapserklaeringProsesseresNormalt() {

      var url = initSynkronisereSigneringsstatusForFarIFarskapserklaering() + 10;
      // when
      var respons =
          httpHeaderTestRestTemplateAsynkron.exchange(
              initSynkronisereSigneringsstatusForFarIFarskapserklaering() + 10,
              HttpMethod.PUT,
              initHttpEntity(null, null),
              Void.class);

      // then
      assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void skalReturnereHttpStatusGoneVedEsigneringStatusFeiletException() {

      // given
      var esigneringStatusFeiletException =
          new EsigneringStatusFeiletException(Feilkode.ESIGNERING_STATUS_FEILET);
      doThrow(esigneringStatusFeiletException)
          .when(farskapsportalService)
          .synkronisereSigneringsstatusFar(anyInt());

      // when
      var respons =
          httpHeaderTestRestTemplateAsynkron.exchange(
              initSynkronisereSigneringsstatusForFarIFarskapserklaering() + 10,
              HttpMethod.PUT,
              initHttpEntity(null, null),
              Void.class);

      // then
      assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    private String initSynkronisereSigneringsstatusForFarIFarskapserklaering() {
      return getBaseUrlForStubs() + "/api/v1/asynkron/statussynkronisering/farskapserklaering/";
    }
  }

  @Nested
  @DisplayName("Tester for endepunkt henteAktoerid")
  class HenteAktoerid {

    @Test
    void skalReturnereAktoeridForPerson() {

      // given
      var personident = "1234";
      var aktoerident = "405060";

      Mockito.when(personopplysningService.henteAktoerid(personident))
          .thenReturn(Optional.of(aktoerident));

      // when
      var respons =
          httpHeaderTestRestTemplateAsynkron.exchange(
              initHenteAktoeridForPerson(),
              HttpMethod.POST,
              initHttpEntity(HenteAktoeridRequest.builder().personident(personident).build()),
              String.class);

      // then
      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(respons.getBody()).isEqualTo(aktoerident));
    }

    @Test
    void skalGiHttpStatusNoContentDersomAktoeridMaglerForPerson() {

      // given
      var personident = "1234";

      Mockito.when(personopplysningService.henteAktoerid(personident)).thenReturn(Optional.empty());

      // when
      var respons =
          httpHeaderTestRestTemplateAsynkron.exchange(
              initHenteAktoeridForPerson(),
              HttpMethod.POST,
              initHttpEntity(HenteAktoeridRequest.builder().personident(personident).build()),
              String.class);

      // then
      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT),
          () -> assertThat(respons.getBody()).isNull());
    }

    private String initHenteAktoeridForPerson() {
      return getBaseUrlForStubs() + "/api/v1/asynkron/aktoerid/hente";
    }
  }

  private String getBaseUrlForStubs() {
    return "http://localhost:" + localServerPort;
  }

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

  private static class CustomHeader {

    String headerName;
    String headerValue;

    CustomHeader(String headerName, String headerValue) {
      this.headerName = headerName;
      this.headerValue = headerValue;
    }
  }
}
