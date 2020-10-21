package no.nav.farskapsportal.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@Builder
@Getter
public class SignaturDto {
  String signatureier;
  boolean harSignert;
  LocalDateTime tidspunktForSignering;
}
