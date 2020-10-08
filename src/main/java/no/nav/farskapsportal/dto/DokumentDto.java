package no.nav.farskapsportal.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class DokumentDto {
  String dokumentnavn;
  byte[] dokument;
  boolean signertAvMor = false;
  boolean signertAvFar = false;
}
