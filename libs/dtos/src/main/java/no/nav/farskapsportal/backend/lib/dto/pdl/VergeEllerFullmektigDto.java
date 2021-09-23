package no.nav.farskapsportal.backend.lib.dto.pdl;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VergeEllerFullmektigDto implements PdlDto {

  String omfang;
}
