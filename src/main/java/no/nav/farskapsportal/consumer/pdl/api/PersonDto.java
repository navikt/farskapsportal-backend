package no.nav.farskapsportal.consumer.pdl.api;

import java.util.ArrayList;
import java.util.List;
import lombok.Singular;
import lombok.Value;

@Value
public class PersonDto {

  List<FamilierelasjonerDto> familierelasjoner = new ArrayList<>();
  List<FoedselDto> foedsel = new ArrayList<>();
  List<KjoennDto> kjoenn = new ArrayList<>();

  @Singular("navn")
  List<NavnDto> navn = new ArrayList<>();
}
