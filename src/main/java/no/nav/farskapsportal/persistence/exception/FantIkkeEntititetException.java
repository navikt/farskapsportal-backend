package no.nav.farskapsportal.persistence.exception;

import no.nav.farskapsportal.exception.UnrecoverableException;

public class FantIkkeEntititetException extends UnrecoverableException {
  private final String message;

  public FantIkkeEntititetException(String message) {
    super(message);
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }
}
