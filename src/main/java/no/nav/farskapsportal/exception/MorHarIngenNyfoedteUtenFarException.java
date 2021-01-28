package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class MorHarIngenNyfoedteUtenFarException extends OppretteFarskapserklaeringException {

  public MorHarIngenNyfoedteUtenFarException(Feilkode feilkode) {
    super(feilkode);
  }
}
