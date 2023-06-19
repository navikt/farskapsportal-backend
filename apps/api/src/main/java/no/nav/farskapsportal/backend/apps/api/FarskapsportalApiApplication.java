package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@EnableSecurityConfiguration
@ConfigurationPropertiesScan("no.nav.farskapsportal.backend.apps.api.config.egenskaper")
@ComponentScan({"no.nav.farskapsportal.backend.apps.api", "no.nav.farskapsportal.backend.libs"})
public class FarskapsportalApiApplication {

  public static final String ISSUER_SELVBETJENING = "selvbetjening";
  public static final String ISSUER_AZURE_AD = "aad";

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_LIVE : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }
}
