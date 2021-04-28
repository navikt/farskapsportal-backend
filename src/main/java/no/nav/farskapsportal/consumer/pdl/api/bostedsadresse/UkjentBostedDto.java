package no.nav.farskapsportal.consumer.pdl.api.bostedsadresse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UkjentBostedDto {
  String bostedskommune;
}
