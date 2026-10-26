package no.nav.farskapsportal.backend.libs.felles;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = ManagementWebSecurityAutoConfiguration.class)
@ComponentScan("no.nav.farskapsportal.backend")
@EntityScan("no.nav.farskapsportal.backend.libs.entity")
public class FarskapsportalFellesTestConfig {}
