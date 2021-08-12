package no.nav.farskapsportal.consumer.dokarkiv.api;

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
