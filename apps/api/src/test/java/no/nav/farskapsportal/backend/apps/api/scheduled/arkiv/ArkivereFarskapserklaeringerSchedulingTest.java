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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@ActiveProfiles(PROFILE_SCHEDULED_TEST)
@SpringJUnitConfig(ScheduledConfig.class)
@SpringBootTest(
    classes = FarskapsportalApiApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class ArkivereFarskapserklaeringerSchedulingTest {

  // Deaktiverer diverse bønner
  private @MockitoBean OAuth2AccessTokenResponse oAuth2AccessTokenResponse;
  private @MockitoBean no.digipost.signature.client.ClientConfiguration clientConfiguration;
  private @MockitoBean no.digipost.signature.client.direct.DirectClient directClient;
  private @MockitoBean PdlApiConsumer pdlApiConsumer;
  private @MockitoBean OppgaveApiConsumer oppgaveApiConsumer;
  private @MockitoBean GcpStorageManager gcpStorageManager;

  private @MockitoBean PersistenceService persistenceService;
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
