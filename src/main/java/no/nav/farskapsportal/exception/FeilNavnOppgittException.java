package no.nav.farskapsportal.exception;

import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.dto.StatusKontrollereFarDto;

@Getter
@Setter
public class FeilNavnOppgittException extends ValideringException {

  private String oppgittNavn;
  private String navnIRegister;

  private Optional<StatusKontrollereFarDto> statusKontrollereFarDto;

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

  public FeilNavnOppgittException(Feilkode feilkode){
    super(feilkode);
  }
}
