package no.nav.farskapsportal.backend.asynkron.consumer.joark.api;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Tilleggsopplysning {
  @NotNull(message = "Tilleggsopplysning mangler nokkel")
  private String nokkel;

  @NotNull(message = "Tilleggsopplysning mangler verdi")
  private String verdi;
}
