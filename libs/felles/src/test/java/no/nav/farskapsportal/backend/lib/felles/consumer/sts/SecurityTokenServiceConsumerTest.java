package no.nav.farskapsportal.backend.lib.felles.consumer.sts;

import static no.nav.farskapsportal.backend.lib.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import no.nav.farskapsportal.backend.lib.felles.config.FarskapsportalFellesConfig;
import no.nav.farskapsportal.backend.lib.felles.test.stub.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = {FarskapsportalFellesConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("SecurityTokenServiceConsumer")
@AutoConfigureWireMock(port = 8096)
public class SecurityTokenServiceConsumerTest {

  @Autowired
  private SecurityTokenServiceConsumer securityTokenServiceConsumer;

  @Autowired
  private StsStub stsStub;

  @Test
  @DisplayName("Feiler hvis brukernavn/ passord mangler")
  void feilerHvisBrukernavnEllerPassordMangler() {
    assertAll(
        () ->
            assertThatIllegalArgumentException()
                .as("brukernvan mangler")
                .isThrownBy(
                    () ->
                        securityTokenServiceConsumer.hentIdTokenForServicebruker(null, "hemmelig")),
        () ->
            assertThatIllegalArgumentException()
                .as("passord mangler")
                .isThrownBy(
                    () ->
                        securityTokenServiceConsumer.hentIdTokenForServicebruker(
                            "srvbdarkivering", null)));
  }

  @Test
  @DisplayName("Skal hente token for servicebruker")
  void skalHenteTokenForServicebruker() {
    var idTokenMock = "eyAgakjgaalkjag";
    stsStub.runSecurityTokenServiceStub(idTokenMock);
    var returnertIdToken =
        securityTokenServiceConsumer.hentIdTokenForServicebruker("srvtest", "hemmelig");
    assertThat(returnertIdToken).isEqualTo(idTokenMock);
  }
}
