package no.nav.farskapsportal.backend.lib.dto.pdl;

public interface PdlDto {

  default MetadataDto getMetadata() {
    return null;
  }

  default FolkeregistermetadataDto getFolkeregistermetadata() {
    return null;
  }
}
