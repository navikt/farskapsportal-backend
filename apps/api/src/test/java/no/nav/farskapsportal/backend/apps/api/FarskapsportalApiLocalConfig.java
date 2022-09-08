package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_REMOTE_POSTGRES;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.stub.DifiESignaturStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES, PROFILE_INTEGRATION_TEST})
@Configuration
@AutoConfigureWireMock(port = 0)
public class FarskapsportalApiLocalConfig {

  public static final String PADES = "/pades";
  public static final String XADES = "/xades";

  @Autowired
  private DifiESignaturStub difiESignaturStub;
/*
  @Bean
  public Options wireMockOptions() {
    final WireMockConfiguration options = WireMockSpring.options();
    options.port(0);
    return options;
  }
*/
  @Bean
  @Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
  public void runStubs() {
    difiESignaturStub.runGetSignedDocument(PADES);
    difiESignaturStub.runGetXades(XADES);
  }
}
