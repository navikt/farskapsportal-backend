package no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain;

import lombok.Value;

@Value
public class ActuatorHealth {

  Status status;
  Components components;
  String[] groups;
}
