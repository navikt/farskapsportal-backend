package no.nav.farskapsportal.backend.libs.felles;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan("no.nav.farskapsportal.backend")
@EntityScan("no.nav.farskapsportal.backend.libs.entity")
public class FarskapsportalFellesTestConfig {

}
