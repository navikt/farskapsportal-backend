package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternFeilException extends UnrecoverableException {

  private final Feilkode feilkode;

  public InternFeilException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
