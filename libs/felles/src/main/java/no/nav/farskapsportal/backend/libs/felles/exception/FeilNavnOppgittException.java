package no.nav.farskapsportal.backend.libs.felles.exception;

import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.dto.StatusKontrollereFarDto;

@Getter
@Setter
public class FeilNavnOppgittException extends ValideringException {

  private String oppgittNavn;
  private String navnIRegister;

  private Optional<StatusKontrollereFarDto> statusKontrollereFarDto;

  public FeilNavnOppgittException(Feilkode feilkode) {
    super(feilkode);
  }
}
