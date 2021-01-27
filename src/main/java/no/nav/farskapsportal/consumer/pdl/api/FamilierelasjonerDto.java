package no.nav.farskapsportal.consumer.pdl.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FamilierelasjonerDto implements PdlDto {

  String relatertPersonsIdent;
  FamilierelasjonRolle relatertPersonsRolle;
  FamilierelasjonRolle minRolleForPerson;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
