package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PersonIkkeFunnetException extends UnrecoverableException {

  public PersonIkkeFunnetException(String message) {
    super(message);
  }


}
