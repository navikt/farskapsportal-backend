package no.nav.farskapsportal.backend.apps.api.consumer.pdl;

import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.UnrecoverableException;

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
