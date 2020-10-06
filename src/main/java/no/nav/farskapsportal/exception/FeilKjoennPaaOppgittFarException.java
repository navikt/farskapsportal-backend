package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FeilKjoennPaaOppgittFarException extends UnrecoverableException {
  public FeilKjoennPaaOppgittFarException(String message) {
    super(message);
  }
}
