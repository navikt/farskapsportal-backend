package no.nav.farskapsportal.backend.libs.dto.pdl;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class FødestedDto implements PdlDto {

  String fødested;
  String fødekommune;
  String fødeland;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
