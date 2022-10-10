package no.nav.farskapsportal.backend.apps.asynkron.consumer.oppgave;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronApplication;
import no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig;
import no.nav.farskapsportal.backend.apps.asynkron.config.RestTemplateAsynkronConfig;
import no.nav.farskapsportal.backend.libs.dto.oppgave.Oppgaveforespoersel;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 0)
@SpringBootTest(classes = {FarskapsportalAsynkronApplication.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {RestTemplateAsynkronConfig.class, FarskapsportalAsynkronConfig.class})
public class OppgaveApiConsumerTest {

  @Autowired
  private OppgaveApiConsumer oppgaveApiConsumer;

  @MockBean
  private OAuth2AccessTokenService oAuth2AccessTokenService;

  @MockBean
  private OAuth2AccessTokenResponse oAuth2AccessTokenResponse;

  @Test
  void skalOppretteOppgave() {

    // given
    var oppgaveIdIStub = 50;  // Se mappings/oppgave-stub.json
    var oppgaveforespoersel = new Oppgaveforespoersel().toBuilder()
        .aktoerId("123")
        .beskrivelse("testing 1-2").build();

    when(oAuth2AccessTokenService.getAccessToken(any())).thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, null));

    // when
    var oppgaveid = oppgaveApiConsumer.oppretteOppgave(oppgaveforespoersel);

    // then
    assertThat(oppgaveid).isEqualTo(oppgaveIdIStub);
  }
}
