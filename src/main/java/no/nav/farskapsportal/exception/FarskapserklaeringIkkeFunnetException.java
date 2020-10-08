package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FarskapserklaeringIkkeFunnetException extends UnrecoverableException {
  public FarskapserklaeringIkkeFunnetException(String msg) {
    super(msg);
  }
}
