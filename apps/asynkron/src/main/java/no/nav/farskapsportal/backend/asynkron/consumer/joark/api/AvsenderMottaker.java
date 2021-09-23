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
public class AvsenderMottaker {

  private String id;

  private AvsenderMottakerIdType idType;

  @NotNull(message = "AvsenderMottaker mangler navn")
  private String navn;

  private String land;
}

