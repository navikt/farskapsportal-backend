package no.nav.farskapsportal.exception;

import no.nav.farskapsportal.api.Feilkode;

public class FeilIDatagrunnlagException extends UnrecoverableException {

  public final static String message = "Feil i datagrunnlag";
  private final Feilkode feilkode;

  public FeilIDatagrunnlagException(Feilkode feilkode) {
    super(feilkode.getBeskrivelse() != null && feilkode.getBeskrivelse().length() > 0 ? feilkode.getBeskrivelse() : message);
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
