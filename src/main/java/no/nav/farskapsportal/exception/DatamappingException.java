package no.nav.farskapsportal.exception;

import lombok.Getter;
import no.nav.farskapsportal.api.Feilkode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class DatamappingException extends UnrecoverableException {
  private final Feilkode feilkode;
  private final Exception originalException;

  public DatamappingException(Feilkode feilkode, Exception originalException) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
    this.originalException = originalException;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }

}
