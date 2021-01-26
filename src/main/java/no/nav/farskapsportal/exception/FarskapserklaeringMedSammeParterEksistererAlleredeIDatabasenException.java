package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException extends UnrecoverableException {

  public final static String message = "Farskasperklæring med samme mor far og barn eksisterer allerede i databasen!";
  private final Feilkode feilkode;


  public FarskapserklaeringMedSammeParterEksistererAlleredeIDatabasenException(Feilkode feilkode) {
    super(message);
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
