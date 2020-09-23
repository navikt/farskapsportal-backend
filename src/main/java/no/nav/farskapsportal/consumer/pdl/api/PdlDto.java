package no.nav.farskapsportal.consumer.pdl.api;

public interface PdlDto {

  default MetadataDto getMetadata() {
    return null;
  }

  default FolkeregistermetadataDto getFolkeregistermetadata() {
    return null;
  }
}
