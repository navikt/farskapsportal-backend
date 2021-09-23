package farskapsportal.asynkron;

import static no.nav.farskapsportal.backend.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_LOCAL;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.asynkron.FarskapsportalAsynkronApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootApplication
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = FarskapsportalAsynkronApplication.class)})
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    topics = {"aapen-brukernotifikasjon-nyBeskjed-v1", "aapen-brukernotifikasjon-done-v1", "aapen-brukernotifikasjon-nyOppgave-v1"})
@Slf4j
public class FarskapsportalStubRunner {


  public static void main(String... args) {

    String profile = args.length < 1 ? PROFILE_LOCAL : args[0];

    SpringApplication app = new SpringApplication(FarskapsportalAsynkronApplication.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }

}
