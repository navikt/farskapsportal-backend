package no.nav.farskapsportal.consumer.pdf;

import no.nav.farskapsportal.exception.UnrecoverableException;

public class PdfgenereringFeiletException extends UnrecoverableException {
  public PdfgenereringFeiletException(String msg) {
    super(msg);
  }
}
