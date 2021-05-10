package no.nav.farskapsportal.consumer.pdl.api.bostedsadresse;

import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.consumer.pdl.api.PdlDto;

@Value
@Builder
public class MatrikkeladresseDto implements PdlDto {

  String bruksenhetsnummer;
  String tilleggsnavn;
  String postnummer;
  String kommunenummer;

}
