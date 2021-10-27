package no.nav.farskapsportal.backend.libs.felles.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class OppretteSigneringsjobbException extends UnrecoverableException {

  public final static String message = "Feil ved opprettelse av signeringsjobb";
  private final Feilkode feilkode;

  public OppretteSigneringsjobbException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse() != null && feilkode.getBeskrivelse().length() > 0 ? feilkode.getBeskrivelse() : message);
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }

}
