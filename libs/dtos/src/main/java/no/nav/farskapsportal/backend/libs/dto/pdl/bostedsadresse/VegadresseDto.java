package no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse;

import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.dto.pdl.PdlDto;

@Value
@Builder
public class VegadresseDto implements PdlDto {

  String husnummer;
  String husbokstav;
  String adressenavn;
  String postnummer;
}
