package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class FoedselsdatoDto implements PdlDto {

  LocalDate foedselsdato;
  Integer foedselsaar;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
