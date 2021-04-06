package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;
import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_SCHEDULED_TEST;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Profile({PROFILE_LIVE, PROFILE_SCHEDULED_TEST, "test"})
@Configuration
@EnableScheduling
@ComponentScan
public class ScheduledConfig {

}
