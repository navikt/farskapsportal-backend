package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class OppretteFarskapserklaeringException extends UnrecoverableException {

  public final static String message = "Feil ved opprettelse av farskapserklæring";
  private final Feilkode feilkode;

  public OppretteFarskapserklaeringException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse() != null && feilkode.getBeskrivelse().length() > 0 ? feilkode.getBeskrivelse() : message);
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }

}
