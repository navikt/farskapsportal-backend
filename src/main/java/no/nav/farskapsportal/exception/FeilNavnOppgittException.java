package no.nav.farskapsportal.exception;

import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.dto.StatusKontrollereFarDto;

@Getter
@Setter
public class FeilNavnOppgittException extends ValideringException {

  private Optional<StatusKontrollereFarDto> statusKontrollereFarDto;

  public FeilNavnOppgittException() {
    super(Feilkode.NAVN_STEMMER_IKKE_MED_REGISTER);
  }
}
