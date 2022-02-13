package no.nav.farskapsportal.backend.libs.dto.pdl;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KjoennDto implements PdlDto {

  KjoennType kjoenn;
  MetadataDto metadata;
}
