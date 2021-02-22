package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class HentingAvDokumentFeiletException extends UnrecoverableException {
  private String message;

  public HentingAvDokumentFeiletException(String message) {
    super(message);
    this.message = message;
  }

  public HentingAvDokumentFeiletException(String message, Exception e) {
    super(message, e);
    e.printStackTrace();
    this.message = message;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
