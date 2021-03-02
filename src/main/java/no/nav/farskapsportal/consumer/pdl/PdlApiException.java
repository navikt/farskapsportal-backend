package no.nav.farskapsportal.consumer.pdl;

import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.exception.UnrecoverableException;

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
