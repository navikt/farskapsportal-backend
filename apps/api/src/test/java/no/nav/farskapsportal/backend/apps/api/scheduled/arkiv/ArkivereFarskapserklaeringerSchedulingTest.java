package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_SCHEDULED_TEST;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplication;
import no.nav.farskapsportal.backend.apps.api.config.ScheduledConfig;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
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
@SpringBootTest(
    classes = FarskapsportalApiApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class ArkivereFarskapserklaeringerSchedulingTest {

  // Deaktiverer diverse bønner
  private @MockBean OAuth2AccessTokenResponse oAuth2AccessTokenResponse;
  private @MockBean no.digipost.signature.client.ClientConfiguration clientConfiguration;
  private @MockBean no.digipost.signature.client.direct.DirectClient directClient;
  private @MockBean PdlApiConsumer pdlApiConsumer;
  private @MockBean OppgaveApiConsumer oppgaveApiConsumer;
  private @MockBean @Qualifier("oppgave") RestTemplate oppgaveRestTemplate;
  private @MockBean @Qualifier("farskapsportal-api") RestTemplate farskapsportalApiRestTemplate;
  private @MockBean GcpStorageManager gcpStorageManager;

  private @MockBean PersistenceService persistenceService;
  private @Autowired FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Test
  public void skalKjoereJobbForArkiveringAvFarskapserklaeringer() throws InterruptedException {

    // given
    var arkiveringsintervall =
        farskapsportalAsynkronEgenskaper.getArkiv().getArkiveringsintervall();

    // when
    Thread.sleep(arkiveringsintervall * 2);

    // then
    verify(persistenceService, atLeast(1))
        .henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt();
  }
}
