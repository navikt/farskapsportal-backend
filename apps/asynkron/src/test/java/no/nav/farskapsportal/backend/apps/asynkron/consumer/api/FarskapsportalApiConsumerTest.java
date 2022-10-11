package no.nav.farskapsportal.backend.apps.asynkron.consumer.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.val;
import no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig;
import no.nav.farskapsportal.backend.apps.asynkron.config.RestTemplateAsynkronConfig;
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
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {RestTemplateAsynkronConfig.class, FarskapsportalAsynkronConfig.class})
public class FarskapsportalApiConsumerTest {

  @Autowired
  private FarskapsportalApiConsumer farskapsportalApiConsumer;

  @MockBean
  private OAuth2AccessTokenService oAuth2AccessTokenService;
  @MockBean
  private OAuth2AccessTokenResponse oAuth2AccessTokenResponse;

  @Test
  void skalHenteAktoerid() {

    // given
    val aktoeridFraStub = "505060601010"; // se mappings/farskapsportal-api-stub.json
    var personident =  LocalDate.now().minusYears (28).format(DateTimeFormatter.ofPattern("ddMMyy"))  + "23154";
    when(oAuth2AccessTokenService.getAccessToken(any())).thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, null));

    // when
    var aktoerid = farskapsportalApiConsumer.henteAktoerid(personident);

    // then
    assertAll(
        () -> assertThat(aktoerid).isPresent(),
        () -> assertThat(aktoerid.get()).isEqualTo(aktoeridFraStub)
    );
  }
}
