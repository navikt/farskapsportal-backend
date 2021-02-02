package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;

@ApiModel
@Value
@Builder
public class BrukerinformasjonResponse {

  @ApiModelProperty(value = "Feilkode som påvirker tilgang", example = "false")
  Optional<Feilkode> feilkodeTilgang;
  @ApiModelProperty(value = "Personen kan initiere prosessen for å erklære farskap", example = "true")
  boolean kanOppretteFarskapserklaering;
  @ApiModelProperty(value = "Personen har en foreldrerolle som er støttet av løsningen", example = "true")
  boolean gyldigForelderrolle;
  @ApiModelProperty(value = "Personens forederrolle", example = "MOR")
  Forelderrolle forelderrolle;
  @ApiModelProperty("Påbegynte farskapserklæringer som venter på fars signatur")
  Set<FarskapserklaeringDto> farsVentendeFarskapserklaeringer;
  @ApiModelProperty("Påbegynte farskapserklæringer som venter på mors signatur (signering avbrutt)")
  Set<FarskapserklaeringDto> morsVentendeFarskapserklaeringer;
  @ApiModelProperty(value = "Mors nyfødte barn som ikke har registrert farskap", example = "{01010112345}")
  Set<String> fnrNyligFoedteBarnUtenRegistrertFar;

}
