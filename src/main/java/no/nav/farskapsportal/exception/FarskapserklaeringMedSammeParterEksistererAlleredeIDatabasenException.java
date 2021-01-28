package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class FarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException extends OppretteFarskapserklaeringException {

  public FarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException(Feilkode feilkode) {
    super(feilkode);
  }
}
