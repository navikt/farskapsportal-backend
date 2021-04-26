package no.nav.farskapsportal.consumer.pdl.api.bostedsadresse;

import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.consumer.pdl.api.PdlDto;

@Value
@Builder
public class VegadresseDto implements PdlDto {

  String husnummer;
  String husbokstav;
  String adressenavn;
  String postnummer;
}
