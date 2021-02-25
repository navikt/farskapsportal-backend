package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class EksisterendeFarskapserklaeringException extends OppretteFarskapserklaeringException {

  public EksisterendeFarskapserklaeringException(Feilkode feilkode) {
    super(feilkode);
  }
}
