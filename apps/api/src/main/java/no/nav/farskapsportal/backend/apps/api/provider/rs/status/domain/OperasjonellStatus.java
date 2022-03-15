package no.nav.farskapsportal.backend.apps.api.provider.rs.status.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OperasjonellStatus {
  String serviceId;
  String status;
  String team;
  String timestamp;
}