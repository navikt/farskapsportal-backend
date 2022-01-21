package no.nav.farskapsportal.backend.libs.dto.status;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OperasjonellStatus {
  String name;
  String status;
  String team;
  String timestamp;
}