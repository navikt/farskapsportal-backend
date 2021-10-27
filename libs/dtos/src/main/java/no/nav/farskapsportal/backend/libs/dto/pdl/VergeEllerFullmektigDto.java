package no.nav.farskapsportal.backend.libs.dto.pdl;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VergeEllerFullmektigDto implements PdlDto {

  String omfang;
}
