package no.nav.farskapsportal.consumer.pdl.api.bostedsadresse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.consumer.pdl.api.FolkeregistermetadataDto;
import no.nav.farskapsportal.consumer.pdl.api.MetadataDto;
import no.nav.farskapsportal.consumer.pdl.api.PdlDto;

@Value
@Builder
public class BostedsadresseDto implements PdlDto {

  LocalDate angittFlyttedato;
  LocalDateTime gyldigTilOgMed;
  String coAdressenavn;
  VegadresseDto vegadresse;
  MatrikkeladresseDto matrikkeladresse;
  UtenlandskAdresseDto utenlandskAdresse;
  UkjentBostedDto ukjentBosted;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
