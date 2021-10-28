package no.nav.farskapsportal.backend.libs.felles.exception;


public class UnrecoverableException extends RuntimeException {

  private final String message;

  public UnrecoverableException(String message) {
    super(message);
    this.message = message;
  }

  public UnrecoverableException(String message, Exception e) {
    super(message, e);
    this.message = message;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
