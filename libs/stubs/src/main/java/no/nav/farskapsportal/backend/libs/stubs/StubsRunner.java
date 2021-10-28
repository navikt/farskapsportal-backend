package no.nav.farskapsportal.backend.libs.stubs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StubsRunner {

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(StubsRunner.class);
    app.run();
  }
}
