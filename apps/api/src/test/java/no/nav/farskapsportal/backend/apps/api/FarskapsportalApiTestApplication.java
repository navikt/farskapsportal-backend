package no.nav.farskapsportal.backend.apps.api;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.*;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;

@Slf4j
@EnableAutoConfiguration
@ComponentScan("no.nav.farskapsportal.backend")
@SpringBootTest(
    classes = FarskapsportalApiApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FarskapsportalApiTestApplication {

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_TEST : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiTestApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }
}
