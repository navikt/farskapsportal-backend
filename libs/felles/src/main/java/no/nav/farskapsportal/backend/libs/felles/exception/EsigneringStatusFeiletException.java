package no.nav.farskapsportal.backend.libs.felles.exception;

import java.util.Optional;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;

public class EsigneringStatusFeiletException extends EsigneringConsumerException {

  private final Optional<Farskapserklaering> farskapserklaering;
  private final Feilkode feilkode;

  public EsigneringStatusFeiletException(Feilkode feilkode) {
    super(feilkode);
    this.feilkode = feilkode;
    this.farskapserklaering = Optional.empty();
  }

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

