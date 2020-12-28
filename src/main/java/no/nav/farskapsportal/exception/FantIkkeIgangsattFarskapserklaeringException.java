package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FantIkkeIgangsattFarskapserklaeringException extends UnrecoverableException {

  private final String message;

  public FantIkkeIgangsattFarskapserklaeringException(String message) {
    super(message);
    this.message = message;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
