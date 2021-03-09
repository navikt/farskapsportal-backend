package no.nav.farskapsportal.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FarskapserklaeringFeilResponse {
  Feilkode feilkode;
  String feilkodebeskrivelse;
<<<<<<< HEAD
  int antallResterendeForsoek;
=======
>>>>>>> main
}
