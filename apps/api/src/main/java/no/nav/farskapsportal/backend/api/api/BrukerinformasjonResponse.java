package no.nav.farskapsportal.backend.api.api;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.lib.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.backend.lib.dto.Forelderrolle;

@Schema
@Value
@Builder
public class BrukerinformasjonResponse {

  @Parameter(description = "Brukers fornavn som registrert i Folkeregisteret", example = "Kari Nordmann")
  String brukersFornavn;
  @Parameter(description = "Personen kan initiere prosessen for å erklære farskap", example = "true")
  boolean kanOppretteFarskapserklaering;
  @Parameter(description = "Personen har en foreldrerolle som er støttet av løsningen", example = "true")
  boolean gyldigForelderrolle;
  @Parameter(description = "Personens forederrolle", example = "MOR")
  Forelderrolle forelderrolle;
  @Parameter(description = "Farskapserklæringer som avventer signering av pålogget bruker")
  Set<FarskapserklaeringDto> avventerSigneringBruker;
  @Parameter(description = "Farskapserklæringer som avventer signering av mottpart til pålogget bruker")
  Set<FarskapserklaeringDto> avventerSigneringMotpart;
  @Parameter(description = "Farskapserklæringer som avventer registrering hos Skatt")
  Set<FarskapserklaeringDto> avventerRegistrering;
  @Parameter(description = "Mors nyfødte barn som ikke har registrert farskap", example = "{01010112345}")
  Set<String> fnrNyligFoedteBarnUtenRegistrertFar;
}
