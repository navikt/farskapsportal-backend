package no.nav.farskapsportal.backend.libs.felles.exception;

public class BucketConsumerException extends UnrecoverableException {

  private final Feilkode feilkode;

  public BucketConsumerException(Feilkode feilkode, Exception e) {
    super(feilkode.getBeskrivelse(), e);
    e.printStackTrace();
    this.feilkode = feilkode;
  }

  public BucketConsumerException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
