package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class FoedselDto implements PdlDto {

  LocalDate foedselsdato;
  String foedeland;
  String foedested;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
