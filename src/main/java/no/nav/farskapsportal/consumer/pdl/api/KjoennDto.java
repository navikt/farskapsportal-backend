package no.nav.farskapsportal.consumer.pdl.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KjoennDto implements PdlDto {
  KjoennType kjoenn;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
