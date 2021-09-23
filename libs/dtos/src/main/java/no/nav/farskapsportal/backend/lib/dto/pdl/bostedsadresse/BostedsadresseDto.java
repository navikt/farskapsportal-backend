package no.nav.farskapsportal.backend.lib.dto.pdl.bostedsadresse;

import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.lib.dto.pdl.FolkeregistermetadataDto;
import no.nav.farskapsportal.backend.lib.dto.pdl.MetadataDto;
import no.nav.farskapsportal.backend.lib.dto.pdl.PdlDto;

@Value
@Builder
public class BostedsadresseDto implements PdlDto {

  VegadresseDto vegadresse;
  MatrikkeladresseDto matrikkeladresse;
  UtenlandskAdresseDto utenlandskAdresse;
  UkjentBostedDto ukjentBosted;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
