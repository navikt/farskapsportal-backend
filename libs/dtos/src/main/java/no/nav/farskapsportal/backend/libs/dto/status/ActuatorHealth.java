package no.nav.farskapsportal.backend.libs.dto.status;

import lombok.Value;

@Value
public class ActuatorHealth {

  Status status;
  Components components;
  String[] groups;
}
