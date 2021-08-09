package no.nav.farskapsportal.exception;

import java.util.Optional;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.GONE)
public class EsigneringStatusFeiletException extends EsigneringConsumerException {

  private final Optional<Farskapserklaering> farskapserklaering;
  private final Feilkode feilkode;

  public EsigneringStatusFeiletException(Feilkode feilkode, Farskapserklaering farskapserklaering) {
    super(feilkode);
    this.feilkode = feilkode;
    this.farskapserklaering = Optional.of(farskapserklaering);
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }

  public Optional<Farskapserklaering> getFarskapserklaering() {
    return this.farskapserklaering;
  }
}

