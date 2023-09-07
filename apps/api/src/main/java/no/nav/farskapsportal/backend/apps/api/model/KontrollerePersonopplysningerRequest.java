package no.nav.farskapsportal.backend.apps.api.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Schema
@Value
@Builder
public class KontrollerePersonopplysningerRequest {

  public static final String FNR_FAR = "11111122222";

  @NotNull
  @Parameter(
      description = "Fødselsnummer til personen som sjekkes",
      example = "\"" + FNR_FAR + "\"")
  String foedselsnummer;

  @NotNull
  @Parameter(
      description = "Personens fornavn, mellomnavn (hvis aktuelt), og etternavn",
      example = "Rask Karaffel")
  String navn;
}
