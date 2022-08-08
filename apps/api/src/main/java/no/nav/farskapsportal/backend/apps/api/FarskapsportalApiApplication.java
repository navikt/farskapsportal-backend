package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@ConfigurationPropertiesScan("no.nav.farskapsportal.backend.apps.api.config.egenskaper")
@ComponentScan({"no.nav.farskapsportal.backend.apps.api", "no.nav.farskapsportal.backend.libs"})
public class FarskapsportalApiApplication {

  public static final String ISSUER = "selvbetjening";

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_LIVE : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }
}
