package no.nav.farskapsportal.consumer.pdl.api;

import java.util.List;
import lombok.Singular;
import lombok.Value;

@Value
public class PersonDto {

  List<FamilierelasjonerDto> familierelasjoner;
  List<FoedselDto> foedsel;
  List<KjoennDto> kjoenn;

  @Singular("navn")
  List<NavnDto> navn;
}
