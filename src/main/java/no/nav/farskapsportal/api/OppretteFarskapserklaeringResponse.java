package no.nav.farskapsportal.api;

import java.net.URI;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OppretteFarskapserklaeringResponse {

  URI redirectUrlForSigneringMor;
}
