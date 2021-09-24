package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FolkeregistermetadataDto {

  LocalDateTime ajourholdstidspunkt;
  LocalDateTime gyldighetstidspunkt;
  LocalDateTime opphoerstidspunkt;
  String kilde;
  String aarsak;
  Integer sekvens;
}
