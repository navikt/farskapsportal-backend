package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class JournalpostApiConsumerException extends InternFeilException {

  public JournalpostApiConsumerException(Feilkode feilkode) {
    super(feilkode);
  }

  public JournalpostApiConsumerException(Feilkode feilkode, Exception exception) {
    super(feilkode, exception);
  }
}
