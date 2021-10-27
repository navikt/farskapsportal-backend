package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoedsfallDto implements PdlDto {

  LocalDate doedsdato;
  MetadataDto metadata;
}
