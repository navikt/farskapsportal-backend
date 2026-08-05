package no.nav.farskapsportal.backend.apps.api.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;

@Value
@Builder
public class FarskapserklaeringFeilResponse {
  Feilkode feilkode;
  String feilkodebeskrivelse;
  Integer antallResterendeForsoek;
  LocalDateTime tidspunktForNullstillingAvForsoek;
}
