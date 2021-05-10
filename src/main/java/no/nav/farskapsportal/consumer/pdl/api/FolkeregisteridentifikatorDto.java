package no.nav.farskapsportal.consumer.pdl.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FolkeregisteridentifikatorDto implements PdlDto {

  String identifikasjonsnummer;
  String status;
  String type;
  MetadataDto metadata;
}
