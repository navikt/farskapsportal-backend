package no.nav.farskapsportal.backend.api.consumer.esignering;

import no.nav.farskapsportal.backend.lib.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.lib.felles.exception.UnrecoverableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class ESigneringFeilException extends UnrecoverableException {
  private final Feilkode feilkode;

  public ESigneringFeilException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
