package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class NyfoedtErForGammelException extends OppretteFarskapserklaeringException {

  public NyfoedtErForGammelException(Feilkode feilkode) {
    super(feilkode);
  }
}
