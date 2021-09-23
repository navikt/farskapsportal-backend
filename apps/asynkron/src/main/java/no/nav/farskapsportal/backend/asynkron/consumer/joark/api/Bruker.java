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
public class Bruker {
  @NotNull(message = "Bruker mangler idType")
  private BrukerIdType idType;

  @NotNull(message = "Bruker mangler id")
  private String id;
}

