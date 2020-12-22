package no.nav.farskapsportal.consumer.pdl;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
@Getter
public class PersonIkkeFunnetException extends PdlApiException {

  private final String message;

  public PersonIkkeFunnetException(String msg) {
    this.message = msg;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
