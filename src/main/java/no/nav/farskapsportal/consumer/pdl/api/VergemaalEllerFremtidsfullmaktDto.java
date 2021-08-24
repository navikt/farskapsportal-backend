package no.nav.farskapsportal.consumer.pdl.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VergemaalEllerFremtidsfullmaktDto implements PdlDto {

  String type;
  String embete;
  VergeEllerFullmektigDto vergeEllerFullmektig;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
