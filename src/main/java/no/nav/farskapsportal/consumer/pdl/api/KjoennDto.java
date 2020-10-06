package no.nav.farskapsportal.consumer.pdl.api;

import lombok.Value;

@Value
public class KjoennDto implements PdlDto {
  KjoennTypeDto kjoenn;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
