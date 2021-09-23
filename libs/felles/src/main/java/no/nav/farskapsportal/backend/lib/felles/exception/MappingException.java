package no.nav.farskapsportal.backend.lib.felles.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class MappingException extends UnrecoverableException {

  private final String message;

  public MappingException(String message, Exception e) {
    super(message, e);
    e.printStackTrace();
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }

}
