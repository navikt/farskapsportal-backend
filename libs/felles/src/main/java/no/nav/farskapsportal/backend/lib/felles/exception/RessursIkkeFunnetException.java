package no.nav.farskapsportal.backend.lib.felles.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
@Getter
public class RessursIkkeFunnetException extends UnrecoverableException {

  private final Feilkode feilkode;

  public RessursIkkeFunnetException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
