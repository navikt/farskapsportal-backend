package no.nav.farskapsportal.backend.apps.asynkron;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@ComponentScan({"no.nav.farskapsportal.backend.apps.asynkron", "no.nav.farskapsportal.backend.libs"})
@ConfigurationPropertiesScan("no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper")
public class FarskapsportalAsynkronApplication {

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_LIVE : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalAsynkronApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }
}
