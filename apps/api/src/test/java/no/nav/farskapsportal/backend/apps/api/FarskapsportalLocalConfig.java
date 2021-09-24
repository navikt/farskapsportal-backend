package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal.PROFILE_LOCAL;
import static no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal.PROFILE_LOCAL_POSTGRES;
import static no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal.PROFILE_REMOTE_POSTGRES;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.stub.DifiESignaturStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES, FarskapsportalApiApplication.PROFILE_INTEGRATION_TEST})
@Configuration
@AutoConfigureWireMock(port = 8096)
public class FarskapsportalLocalConfig {

  public static final String PADES = "/pades";
  public static final String XADES = "/xades";

  @Autowired
  private DifiESignaturStub difiESignaturStub;

  @Bean
  public Options wireMockOptions() {
    final WireMockConfiguration options = WireMockSpring.options();
    options.port(8096);
    return options;
  }

  @Bean
  @Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES})
  public void runStubs() {
    difiESignaturStub.runGetSignedDocument(PADES);
    difiESignaturStub.runGetXades(XADES);
  }
}
