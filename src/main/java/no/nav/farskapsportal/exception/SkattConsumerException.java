package no.nav.farskapsportal.exception;

import lombok.Getter;
import no.nav.farskapsportal.api.Feilkode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class SkattConsumerException extends UnrecoverableException {
  private final Feilkode feilkode;
  private final Exception originalException;

  public SkattConsumerException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
    this.originalException = this;
  }

  public SkattConsumerException(Feilkode feilkode, Exception originalException) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
    this.originalException = originalException;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }

}
