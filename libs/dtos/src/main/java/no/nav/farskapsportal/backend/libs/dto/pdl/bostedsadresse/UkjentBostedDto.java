package no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UkjentBostedDto {
  String bostedskommune;
}
