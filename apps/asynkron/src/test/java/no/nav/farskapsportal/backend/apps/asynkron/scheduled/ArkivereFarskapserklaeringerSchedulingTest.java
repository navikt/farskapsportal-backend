package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SCHEDULED_TEST;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronApplication;
import no.nav.farskapsportal.backend.apps.asynkron.config.ScheduledConfig;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestTemplate;

@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@ActiveProfiles(PROFILE_SCHEDULED_TEST)
@SpringJUnitConfig(ScheduledConfig.class)
@SpringBootTest(classes = FarskapsportalAsynkronApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ArkivereFarskapserklaeringerSchedulingTest {

  // Deaktiverer diverse bønner
  @MockBean
  private @Qualifier("oppgave") RestTemplate oppgaveRestTemplate;
  @MockBean
  private @Qualifier("farskapsportal-api") RestTemplate farskapsportalApiRestTemplate;
  @MockBean
  private @Qualifier("asynk-base") RestTemplate asynkBaseRestTemplate;

  @MockBean
  private PersistenceService persistenceService;

  @Autowired
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Test
  public void skalKjoereJobbForArkiveringAvFarskapserklaeringer() throws InterruptedException {

    // given
    var arkiveringsintervall = farskapsportalAsynkronEgenskaper.getArkiveringsintervall();

    // when
    Thread.sleep(arkiveringsintervall * 2);

    // then
    verify(persistenceService, atLeast(1)).henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt();
  }

}
