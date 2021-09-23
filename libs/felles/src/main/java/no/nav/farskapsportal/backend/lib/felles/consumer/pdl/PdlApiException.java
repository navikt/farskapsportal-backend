package no.nav.farskapsportal.backend.lib.felles.consumer.pdl;

import no.nav.farskapsportal.backend.lib.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.lib.felles.exception.UnrecoverableException;

public class PdlApiException extends UnrecoverableException {

  private final Feilkode feilkode;

  public PdlApiException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
