package no.nav.farskapsportal.consumer.esignering;

import no.nav.farskapsportal.exception.UnrecoverableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ESigneringFeiletException extends UnrecoverableException {
  public ESigneringFeiletException(String msg) {
    super(msg);
  }
}
