package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class ForskjelligeFedreException extends OppretteFarskapserklaeringException {

  public ForskjelligeFedreException(Feilkode feilkode) {
    super(feilkode);
  }
}
