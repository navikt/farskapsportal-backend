package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import lombok.Value;

@ApiModel
@Value
public class KontrollerePersonopplysningerResponse {
  boolean riktigKombinasjonAvFodselnummerOgNavn;
  String feilmelding;
}
