package no.nav.farskapsportal.consumer.pdl.api;

import java.time.LocalDate;
import lombok.Value;

@Value
public class FoedselDto implements PdlDto {
  LocalDate foedselsdato;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
