package no.nav.farskapsportal;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableJwtTokenValidation
public class FarskapsportalApiApplication {
  public static final String ISSUER = "issuer";
  public static final String PROFILE_LIVE = "live";

  public static void main(String[] args) {

    String profile = args.length < 1 ? PROFILE_LIVE : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalApiApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(profile);
  }
}
