package no.nav.farskapsportal.backend.lib.felles.persistence.exception;

import no.nav.farskapsportal.backend.lib.felles.exception.UnrecoverableException;

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
