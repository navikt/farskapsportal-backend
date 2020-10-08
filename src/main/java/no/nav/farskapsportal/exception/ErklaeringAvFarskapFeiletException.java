package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ErklaeringAvFarskapFeiletException extends UnrecoverableException {

  private static final String feilmelding =
      "Erklæring av farskap feilet. Mulige årsaker er: pålogget person er ikke kvinne, "
          + "eller dersom pålogget person er mann er dette en annen person enn barnefaren";

  public ErklaeringAvFarskapFeiletException() {
    super(feilmelding);
  }
}
