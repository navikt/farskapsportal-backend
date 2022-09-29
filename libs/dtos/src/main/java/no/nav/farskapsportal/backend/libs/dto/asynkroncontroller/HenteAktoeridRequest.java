package no.nav.farskapsportal.backend.libs.dto.asynkroncontroller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Schema
@Value
@Builder
public class HenteAktoeridRequest {

  public static final String PERSONIDENT = "11111122222";

  @NotNull
  @Parameter(description = "Personident til personen aktørid skal hentes for", example = "\"" + PERSONIDENT + "\"")
  String personident;

}
