package no.nav.farskapsportal.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OppretteFarskapserklaeringResponse {

  String redirectUrlForSigneringMor;
}
