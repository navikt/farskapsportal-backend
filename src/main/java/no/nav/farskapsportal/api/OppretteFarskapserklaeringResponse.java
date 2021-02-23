package no.nav.farskapsportal.api;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Getter
public class OppretteFarskapserklaeringResponse extends FarskapserklaeringResponse {

  private String redirectUrlForSigneringMor;

  @Builder
  public OppretteFarskapserklaeringResponse(Optional<Feilkode> feilkode, String redirectUrlForSigneringMor) {
    super(feilkode);
    this.redirectUrlForSigneringMor = redirectUrlForSigneringMor;
  }
}
