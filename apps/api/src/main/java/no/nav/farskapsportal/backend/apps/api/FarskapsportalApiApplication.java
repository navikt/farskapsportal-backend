package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication(
    exclude = {
      SecurityAutoConfiguration.class,
      ManagementWebSecurityAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class,
      ServletWebSecurityAutoConfiguration.class,
    })
@ConfigurationPropertiesScan("no.nav.farskapsportal.backend.apps.api.config.egenskaper")
@ComponentScan({"no.nav.farskapsportal.backend.apps.api", "no.nav.farskapsportal.backend.libs"})
public class FarskapsportalApiApplication {

  public static final String ISSUER_TOKENX = "tokenx";
  public static final String ISSUER_AZURE_AD = "aad";

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_LIVE : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }
}
