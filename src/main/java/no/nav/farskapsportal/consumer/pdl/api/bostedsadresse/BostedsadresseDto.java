package no.nav.farskapsportal.consumer.pdl.api.bostedsadresse;

import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.consumer.pdl.api.FolkeregistermetadataDto;
import no.nav.farskapsportal.consumer.pdl.api.MetadataDto;
import no.nav.farskapsportal.consumer.pdl.api.PdlDto;

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
