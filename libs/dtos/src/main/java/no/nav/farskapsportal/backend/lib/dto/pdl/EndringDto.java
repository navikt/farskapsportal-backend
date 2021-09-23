package no.nav.farskapsportal.backend.lib.dto.pdl;

import java.time.LocalDateTime;
import lombok.Value;

@Value
public class EndringDto {
  String type;
  LocalDateTime registrert;
  String registrertAv;
  String systemkilde;
  String kilde;
}
