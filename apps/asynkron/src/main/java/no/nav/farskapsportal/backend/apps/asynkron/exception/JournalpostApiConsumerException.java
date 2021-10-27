package no.nav.farskapsportal.backend.apps.asynkron.exception;

import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;

public class JournalpostApiConsumerException extends InternFeilException {

  public JournalpostApiConsumerException(Feilkode feilkode) {
    super(feilkode);
  }

  public JournalpostApiConsumerException(Feilkode feilkode, Exception exception) {
    super(feilkode, exception);
  }
}
