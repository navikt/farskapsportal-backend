package no.nav.farskapsportal.consumer.pdl;

import no.nav.farskapsportal.exception.UnrecoverableException;

public class PdlApiException extends UnrecoverableException {
  private static final String default_message = "Det har oppstått en feil i kommunikasjon med PdlApi";

  private final String message;

  public PdlApiException() {
    super(default_message);
    this.message = default_message;
  }

  public PdlApiException(String message) {
    super(message);
    this.message = message;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
