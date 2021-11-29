package no.nav.farskapsportal.backend.apps.api.provider.rs;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;

import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.farskapsportal.backend.libs.felles.exception.EsigneringStatusFeiletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalApiApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class AsynkronControllerTest {

  @LocalServerPort
  private int localServerPort;

  @Autowired
  private HttpHeaderTestRestTemplate httpHeaderTestRestTemplate;

  @MockBean
  private FarskapsportalService farskapsportalService;

  @Test
  void skalGiAcceptedDersomFarskapserklaeringProsesseresNormalt() {

    // when
    var respons = httpHeaderTestRestTemplate.exchange(initSynkronisereSigneringsstatusForFarIFarskapserklaering() + 10, HttpMethod.PUT, null,
        Void.class);

    // then
    assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

  }

  @Test
  void skalReturnereHttpStatusGoneVedEsigneringStatusFeiletException() {

    // given
    doThrow(EsigneringStatusFeiletException.class).when(farskapsportalService).synkronisereSigneringsstatusFar(anyInt());

    // when
    var respons = httpHeaderTestRestTemplate.exchange(initSynkronisereSigneringsstatusForFarIFarskapserklaering() + 10, HttpMethod.PUT, null,
        Void.class);

    // then
    assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.GONE);

  }


  private String initSynkronisereSigneringsstatusForFarIFarskapserklaering() {
    return getBaseUrlForStubs() + "/api/v1/asynkron/statussynkronisering/farskapserklaering/";
  }

  private String getBaseUrlForStubs() {
    return "http://localhost:" + localServerPort;
  }


}
