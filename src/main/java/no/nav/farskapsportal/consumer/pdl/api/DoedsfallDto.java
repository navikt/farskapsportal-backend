package no.nav.farskapsportal.consumer.pdl.api;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoedsfallDto implements PdlDto {

  LocalDate doedsdato;
  MetadataDto metadata;
}
