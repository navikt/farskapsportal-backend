package no.nav.farskapsportal.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class FarskapserklaeringDto {
  private @Setter(AccessLevel.NONE) BarnDto barn;
  private @Setter(AccessLevel.NONE) ForelderDto mor;
  private @Setter(AccessLevel.NONE) ForelderDto far;
  private DokumentDto dokument;
}
