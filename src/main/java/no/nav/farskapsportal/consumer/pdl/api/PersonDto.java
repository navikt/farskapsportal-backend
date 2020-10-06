package no.nav.farskapsportal.consumer.pdl.api;

import java.util.List;
import lombok.Singular;
import lombok.Value;

@Value
public class PersonDto {

  @Singular("kjoenn")
  List<KjoennDto> kjoenn;

  @Singular("navn")
  List<NavnDto> navn;
}
