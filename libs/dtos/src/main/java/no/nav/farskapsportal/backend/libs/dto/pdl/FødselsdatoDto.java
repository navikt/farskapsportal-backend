package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class FødselsdatoDto implements PdlDto {

  LocalDate fødselsdato;
  LocalDate fødselsaar;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
