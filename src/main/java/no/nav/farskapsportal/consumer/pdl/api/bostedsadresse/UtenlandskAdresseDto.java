package no.nav.farskapsportal.consumer.pdl.api.bostedsadresse;

import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.consumer.pdl.api.PdlDto;

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
