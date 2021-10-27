package no.nav.farskapsportal.backend.libs.felles.persistence.exception;

import no.nav.farskapsportal.backend.libs.felles.exception.UnrecoverableException;

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
