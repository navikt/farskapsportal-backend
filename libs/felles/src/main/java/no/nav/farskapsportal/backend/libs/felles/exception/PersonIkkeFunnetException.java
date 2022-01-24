package no.nav.farskapsportal.backend.libs.felles.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Setter
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class PersonIkkeFunnetException extends KontrollereNavnFarException {

  public PersonIkkeFunnetException(String oppgittNavn, String navnIRegister) {
    super(Feilkode.PDL_PERSON_IKKE_FUNNET);
    this.oppgittNavn = oppgittNavn;
    this.navnIRegister = navnIRegister;
  }
}
