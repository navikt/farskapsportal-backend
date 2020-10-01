package no.nav.farskapsportal.consumer.pdl.api;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EndringDto {
  String type;
  LocalDateTime registrert;
  String registrertAv;
  String systemkilde;
  String kilde;
}
