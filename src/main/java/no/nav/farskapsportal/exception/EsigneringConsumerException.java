package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class EsigneringConsumerException extends UnrecoverableException {

  private final String message;

  public EsigneringConsumerException(String message) {
    super(message);
    this.message = message;
  }

  public EsigneringConsumerException(String message, Exception e) {
    super(message, e);
    e.printStackTrace();
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }

}

