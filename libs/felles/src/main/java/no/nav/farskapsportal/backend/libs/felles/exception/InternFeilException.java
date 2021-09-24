package no.nav.farskapsportal.backend.libs.felles.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternFeilException extends UnrecoverableException {

  private final Feilkode feilkode;
  private final Exception originalException;

  public InternFeilException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
    this.originalException = this;
  }

  public InternFeilException(Feilkode feilkode, Exception originalException) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
    this.originalException = originalException;
  }


  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
