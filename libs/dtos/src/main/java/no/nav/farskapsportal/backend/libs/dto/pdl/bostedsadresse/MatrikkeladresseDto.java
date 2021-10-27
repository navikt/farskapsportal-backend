package no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse;

import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.dto.pdl.PdlDto;

@Value
@Builder
public class MatrikkeladresseDto implements PdlDto {

  String bruksenhetsnummer;
  String tilleggsnavn;
  String postnummer;
  String kommunenummer;

}
