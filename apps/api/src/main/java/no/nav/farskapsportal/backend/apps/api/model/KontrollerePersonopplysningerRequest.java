package no.nav.farskapsportal.backend.apps.api.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Schema
@Value
@Builder
public class KontrollerePersonopplysningerRequest {

  @NotNull
  @Parameter(description = "Fødselsnummer til personen som sjekkes")
  String foedselsnummer;

  @NotNull
  @Parameter(
      description = "Personens fornavn, mellomnavn (hvis aktuelt), og etternavn",
      example = "Rask Karaffel")
  String navn;
}
