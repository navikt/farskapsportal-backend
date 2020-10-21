package no.nav.farskapsportal.consumer.pdl;

import java.util.List;
import java.util.StringJoiner;

public class PdlApiErrorException extends PdlApiException {
  private String message;

  public PdlApiErrorException(List<PdlApiError> errors) {
    super();
    StringJoiner stringJoiner = new StringJoiner("\n");
    stringJoiner.add("Error i respons fra pdl-api: ");
    errors.forEach(
        pdlApiError ->
            stringJoiner.add(pdlApiError.getMessage() + " code: " + pdlApiError.getCode()));
    message = stringJoiner.toString();
  }

  @Override
  public String getMessage() {
    return message;
  }
}
