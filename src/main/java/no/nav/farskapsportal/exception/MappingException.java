package no.nav.farskapsportal.exception;

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
