package no.nav.farskapsportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class OppgittNavnStemmerIkkeMedRegistrertNavnException extends UnrecoverableException {

  public OppgittNavnStemmerIkkeMedRegistrertNavnException(String message) {
    super(message);
  }
}
