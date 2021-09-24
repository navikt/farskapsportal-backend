package no.nav.farskapsportal.backend.libs.felles.consumer.pdl;

import java.util.List;
import java.util.StringJoiner;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;

public class PdlApiErrorException extends PdlApiException {
  private String message;

  public PdlApiErrorException(List<PdlApiError> errors) {
    super(Feilkode.PDL_FEIL);
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
