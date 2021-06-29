package no.nav.farskapsportal.exception;

import java.util.Optional;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class EsigneringConsumerException extends UnrecoverableException {

  private final Optional<Farskapserklaering> farskapserklaering;
  private final Feilkode feilkode;

  public EsigneringConsumerException(Feilkode feilkode, Exception e) {
    super(feilkode.getBeskrivelse(), e);
    e.printStackTrace();
    this.feilkode = feilkode;
    this.farskapserklaering = Optional.empty();
  }

  public EsigneringConsumerException(Feilkode feilkode, Farskapserklaering farskapserklaering) {
    super(feilkode.getBeskrivelse());
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

