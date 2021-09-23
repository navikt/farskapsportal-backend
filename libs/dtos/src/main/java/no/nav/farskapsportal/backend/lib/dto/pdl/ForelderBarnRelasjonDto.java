package no.nav.farskapsportal.backend.lib.dto.pdl;

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
