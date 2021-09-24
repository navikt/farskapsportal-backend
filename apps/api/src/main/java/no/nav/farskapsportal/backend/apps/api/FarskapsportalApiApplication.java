package no.nav.farskapsportal.backend.apps.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@ConfigurationPropertiesScan("no.nav.farskapsportal.backend.apps.api.config.egenskaper")
@ComponentScan("no.nav.farskapsportal.backend")
public class FarskapsportalApiApplication {

  public static final String ISSUER = "selvbetjening";
  public static final String PROFILE_LIVE = "live";
  public static final String PROFILE_INTEGRATION_TEST = "integration-test";
  public static final String PROFILE_SCHEDULED_TEST = "scheduled-test";

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_LIVE : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }
}
