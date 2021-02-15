package no.nav.farskapsportal.exception;


public class SigneringsjobbFeiletException extends UnrecoverableException {
  private final String message;

  public SigneringsjobbFeiletException(String message) {
    super(message);
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }
}
