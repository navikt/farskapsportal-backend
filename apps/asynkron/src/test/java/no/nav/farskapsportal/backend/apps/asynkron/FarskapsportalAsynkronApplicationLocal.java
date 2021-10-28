package no.nav.farskapsportal.backend.apps.asynkron;

import static no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SCHEDULED_TEST;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LOCAL_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_REMOTE_POSTGRES;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

@SpringBootApplication
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = {FarskapsportalAsynkronApplication.class})})
@Slf4j
public class FarskapsportalAsynkronApplicationLocal {

  public static void main(String... args) {

    String profile = args.length < 1 ? PROFILE_LOCAL : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalAsynkronApplicationLocal.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }

  @Bean
  @Profile({PROFILE_TEST, PROFILE_LOCAL, PROFILE_LOCAL_POSTGRES, PROFILE_REMOTE_POSTGRES, PROFILE_SCHEDULED_TEST})
  public KeyStoreConfig keyStoreConfig(@Autowired ResourceLoader resourceLoader) throws IOException {
    try (InputStream inputStream = resourceLoader.getClassLoader().getResourceAsStream("esigneringkeystore.jceks")) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke esigneringkeystore.jceks");
      } else {
        return KeyStoreConfig.fromJavaKeyStore(inputStream, "selfsigned", "changeit", "changeit");
      }
    }
  }

}
