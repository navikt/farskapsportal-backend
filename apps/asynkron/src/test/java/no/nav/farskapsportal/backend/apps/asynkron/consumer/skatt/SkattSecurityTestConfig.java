package no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt;

import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

public class SkattSecurityTestConfig extends WebSecurityConfigurerAdapter {

  // Skru av Spring security
  @Override
  public void configure(WebSecurity web) {
    web.ignoring().antMatchers("/**");
  }
}

