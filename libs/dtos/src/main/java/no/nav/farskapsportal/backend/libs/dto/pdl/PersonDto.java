package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.BostedsadresseDto;

@Data
@NoArgsConstructor
public class PersonDto {

  List<DoedsfallDto> doedsfall = new ArrayList<>();
  List<ForelderBarnRelasjonDto> forelderBarnRelasjon = new ArrayList<>();
  List<FoedselDto> foedsel = new ArrayList<>();
  List<FoedselsdatoDto> foedselsdato = new ArrayList<>();
  List<FoedestedDto> foedested = new ArrayList<>();
  List<FolkeregisteridentifikatorDto> folkeregisteridentifikator = new ArrayList<>();
  List<KjoennDto> kjoenn = new ArrayList<>();
  List<BostedsadresseDto> bostedsadresse = new ArrayList<>();
  List<VergemaalEllerFremtidsfullmaktDto> vergemaalEllerFremtidsfullmakt = new ArrayList<>();
  List<NavnDto> navn = new ArrayList<>();
  List<SivilstandDto> sivilstand = new ArrayList<>();
}
