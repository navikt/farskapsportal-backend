package no.nav.farskapsportal.backend.libs.felles.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Setter
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FeilNavnOppgittException extends KontrollereNavnFarException {

  public FeilNavnOppgittException(String oppgittNavn, String navnIRegister) {
    super(Feilkode.NAVN_STEMMER_IKKE_MED_REGISTER);
    this.oppgittNavn = oppgittNavn;
    this.navnIRegister = navnIRegister;
  }

  public FeilNavnOppgittException(Feilkode feilkode, String oppgittNavn, String navnIRegister) {
    super(feilkode);
    this.oppgittNavn = oppgittNavn;
    this.navnIRegister = navnIRegister;
  }
}
