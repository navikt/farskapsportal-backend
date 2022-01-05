package no.nav.farskapsportal.backend.libs.felles.exception;

import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.backend.libs.dto.StatusKontrollereFarDto;

@Getter
@Setter
public abstract class KontrollereNavnFarException extends UnrecoverableException {

  protected Feilkode feilkode;
  protected String oppgittNavn;
  protected String navnIRegister;
  Optional<StatusKontrollereFarDto> statusKontrollereFarDto;

  public KontrollereNavnFarException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
  }
}
