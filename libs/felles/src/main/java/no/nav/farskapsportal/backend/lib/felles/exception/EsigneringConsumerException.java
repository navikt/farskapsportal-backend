package no.nav.farskapsportal.backend.lib.felles.exception;

public class EsigneringConsumerException extends UnrecoverableException {

  private final Feilkode feilkode;

  public EsigneringConsumerException(Feilkode feilkode, Exception e) {
    super(feilkode.getBeskrivelse(), e);
    e.printStackTrace();
    this.feilkode = feilkode;
  }

  public EsigneringConsumerException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
