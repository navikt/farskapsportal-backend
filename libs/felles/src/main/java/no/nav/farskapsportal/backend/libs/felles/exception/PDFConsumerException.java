package no.nav.farskapsportal.backend.libs.felles.exception;

public class PDFConsumerException extends InternFeilException {

  public PDFConsumerException(Feilkode feilkode, Exception exception) {
    super(feilkode, exception);
  }
}
