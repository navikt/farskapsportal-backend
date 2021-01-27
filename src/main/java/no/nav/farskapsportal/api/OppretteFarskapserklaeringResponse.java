package no.nav.farskapsportal.api;

import java.net.URI;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Getter
public class OppretteFarskapserklaeringResponse extends FarskapserklaeringResponse {

  private URI redirectUrlForSigneringMor;

  @Builder
  public OppretteFarskapserklaeringResponse(Optional<Feilkode> feilkode, URI redirectUrlForSigneringMor) {
    super(feilkode);
    this.redirectUrlForSigneringMor = redirectUrlForSigneringMor;
  }
}
