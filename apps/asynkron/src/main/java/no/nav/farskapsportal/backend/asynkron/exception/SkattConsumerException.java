package no.nav.farskapsportal.backend.asynkron.exception;

import no.nav.farskapsportal.backend.lib.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.lib.felles.exception.InternFeilException;

public class SkattConsumerException extends InternFeilException {

  public SkattConsumerException(Feilkode feilkode) {
    super(feilkode);
  }

  public SkattConsumerException(Feilkode feilkode, Exception exception) {
    super(feilkode, exception);
  }
}
