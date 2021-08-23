package no.nav.farskapsportal.consumer.pdl.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VergeEllerFullmektigDto implements PdlDto {

  String omfang;
}
