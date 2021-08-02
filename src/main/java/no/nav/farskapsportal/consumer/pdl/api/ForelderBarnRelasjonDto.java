package no.nav.farskapsportal.consumer.pdl.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ForelderBarnRelasjonDto implements PdlDto {

  String relatertPersonsIdent;
  ForelderBarnRelasjonRolle relatertPersonsRolle;
  ForelderBarnRelasjonRolle minRolleForPerson;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
