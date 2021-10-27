package no.nav.farskapsportal.backend.apps.api.api;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Getter
public class OppretteFarskapserklaeringResponse {

  private String redirectUrlForSigneringMor;

  @Builder
  public OppretteFarskapserklaeringResponse(String redirectUrlForSigneringMor) {
    this.redirectUrlForSigneringMor = redirectUrlForSigneringMor;
  }
}
