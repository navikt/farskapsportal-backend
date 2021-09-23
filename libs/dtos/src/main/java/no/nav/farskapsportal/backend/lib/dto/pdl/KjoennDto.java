package no.nav.farskapsportal.backend.lib.dto.pdl;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KjoennDto implements PdlDto {

  KjoennType kjoenn;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
