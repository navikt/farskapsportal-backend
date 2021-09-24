package no.nav.farskapsportal.backend.apps.api.api;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;

@Value
@Builder
public class FarskapserklaeringFeilResponse {
  Feilkode feilkode;
  String feilkodebeskrivelse;
  Optional<Integer> antallResterendeForsoek;
  LocalDateTime tidspunktForNullstillingAvForsoek;
}
