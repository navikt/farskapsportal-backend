package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FarskapserklaeringIkkeFunnetException extends UnrecoverableException {

  private final String message;

  public FarskapserklaeringIkkeFunnetException(String msg) {
    super(msg);
    this.message = msg;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
