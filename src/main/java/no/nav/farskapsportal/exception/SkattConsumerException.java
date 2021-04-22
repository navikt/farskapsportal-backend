package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class SkattConsumerException extends InternFeilException {

  public SkattConsumerException(Feilkode feilkode) {
    super(feilkode);
  }

  public SkattConsumerException(Feilkode feilkode, Exception exception) {
    super(feilkode, exception);
  }
}
