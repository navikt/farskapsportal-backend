package no.nav.farskapsportal.backend.apps.api.consumer.oppgave;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.config.RestTemplateConfig;
import no.nav.farskapsportal.backend.libs.dto.oppgave.Oppgaveforespoersel;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles(PROFILE_TEST)
@DirtiesContext
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@SpringBootTest(
    classes = FarskapsportalApiApplicationLocal.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = RestTemplateConfig.class)
public class OppgaveApiConsumerTest {

  private @Autowired OppgaveApiConsumer oppgaveApiConsumer;
  private @MockitoBean GcpStorageManager gcpStorageManager;
  private @MockitoBean OAuth2AccessTokenService oAuth2AccessTokenService;
  private @MockitoBean OAuth2AccessTokenResponse oAuth2AccessTokenResponse;

  @Test
  void skalOppretteOppgave() {

    // given
    var oppgaveIdIStub = 50; // Se mappings/oppgave-stub.json
    var oppgaveforespoersel =
        new Oppgaveforespoersel().toBuilder().aktoerId("123").beskrivelse("testing 1-2").build();

    when(oAuth2AccessTokenService.getAccessToken(any()))
        .thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, Collections.emptyMap()));

    // when
    var oppgaveid = oppgaveApiConsumer.oppretteOppgave(oppgaveforespoersel);

    // then
    assertThat(oppgaveid).isEqualTo(oppgaveIdIStub);
  }
}
