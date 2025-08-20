package no.nav.farskapsportal.backend.libs.dto.pdl;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class FoedestedDto implements PdlDto {

  String foedested;
  String foedekommune;
  String foedeland;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
