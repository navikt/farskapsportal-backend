package no.nav.farskapsportal.consumer.pdl.api;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.validation.annotation.Validated;

@Value
@Builder
public class NavnDto implements PdlDto {
  String fornavn;
  String mellomnavn;
  String etternavn;
  String forkortetNavn;
  PersonnavnDto originaltNavn;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
