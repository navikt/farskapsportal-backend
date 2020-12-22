package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PadesUrlIkkeTilgjengeligException extends UnrecoverableException {

  private final String message;

  public PadesUrlIkkeTilgjengeligException(String message) {
    super(message);
    this.message = message;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
