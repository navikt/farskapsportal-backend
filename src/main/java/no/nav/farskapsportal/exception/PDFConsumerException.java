package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;


public class PDFConsumerException extends InternFeilException {

  public PDFConsumerException(Feilkode feilkode, Exception exception) {
    super(feilkode, exception);
  }

}
