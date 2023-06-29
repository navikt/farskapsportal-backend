package no.nav.farskapsportal.backend.apps.api.exception;

import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;

public class SkattConsumerException extends InternFeilException {

  public SkattConsumerException(Feilkode feilkode) {
    super(feilkode);
  }

  public SkattConsumerException(Feilkode feilkode, Exception exception) {
    super(feilkode, exception);
  }
}
