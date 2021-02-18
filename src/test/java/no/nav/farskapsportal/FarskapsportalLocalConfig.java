package no.nav.farskapsportal;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_LOCAL;
import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_LOCAL_POSTGRES;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockSpring;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_INTEGRATION_TEST})
@Configuration
@AutoConfigureWireMock(port = 8096)
public class FarskapsportalLocalConfig {

  @Bean
  public Options wireMockOptions() {

    final WireMockConfiguration options = WireMockSpring.options();
    options.port(8096);

    return options;
  }
}
