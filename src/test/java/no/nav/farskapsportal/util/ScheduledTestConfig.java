package no.nav.farskapsportal.util;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_SCHEDULED_TEST;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Profile(PROFILE_SCHEDULED_TEST)
@Configuration
@EnableScheduling
@ComponentScan
public class ScheduledTestConfig {

}
