package no.nav.farskapsportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FarskapserklaeringDto {
  private BarnDto barn;
  private ForelderDto mor;
  private ForelderDto far;
  private DokumentDto dokument;
}
