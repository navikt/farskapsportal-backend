package no.nav.farskapsportal.api;

import java.util.Optional;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FarskapserklaeringFeilResponse {
  Feilkode feilkode;
  String feilkodebeskrivelse;
  Optional<Integer> antallResterendeForsoek;
}
