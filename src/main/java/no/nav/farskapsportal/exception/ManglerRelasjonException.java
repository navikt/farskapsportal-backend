package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class ManglerRelasjonException extends OppretteFarskapserklaeringException {

  public ManglerRelasjonException(Feilkode feilkode) {
    super(feilkode);
  }
}
