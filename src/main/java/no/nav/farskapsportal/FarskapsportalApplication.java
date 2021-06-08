package no.nav.farskapsportal;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@Slf4j
@SpringBootApplication
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@ConfigurationPropertiesScan("no.nav.farskapsportal.config.egenskaper")
public class FarskapsportalApplication {

  public static final String ISSUER = "selvbetjening";
  public static final String PROFILE_LIVE = "live";
  public static final String PROFILE_INTEGRATION_TEST = "integration-test";

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_LIVE : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }
}
