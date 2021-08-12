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
public class AvsenderMottaker {

  private String id;

  private AvsenderMottakerIdType idType;

  @NotNull(message = "AvsenderMottaker mangler navn")
  private String navn;

  private String land;
}

