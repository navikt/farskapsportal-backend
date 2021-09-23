package no.nav.farskapsportal.backend.lib.dto.pdl.bostedsadresse;

import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.lib.dto.pdl.PdlDto;

@Value
@Builder
public class UtenlandskAdresseDto implements PdlDto {

  String adressenavnNummer;
  String bygningEtasjeLeilighet;
  String postboksNummerNavn;
  String postbkode;
  String bySted;
  String regionDistriktOmraade;
  String landkode;

}
