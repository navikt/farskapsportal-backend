package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EndringDto {

  Type type;
  LocalDateTime registrert;
  String registrertAv;
  String systemkilde;
  String kilde;

  public enum Type {
    OPPRETT, KORRIGER, OPPHOER, ANNULLER
  }
}
