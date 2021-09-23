package no.nav.farskapsportal.backend.lib.dto.pdl.bostedsadresse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UkjentBostedDto {
  String bostedskommune;
}
