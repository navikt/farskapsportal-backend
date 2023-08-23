package no.nav.farskapsportal.backend.libs.felles.exception;

public class FeilIDatagrunnlagException extends UnrecoverableException {

  public static final String message = "Feil i datagrunnlag";
  private final Feilkode feilkode;

  public FeilIDatagrunnlagException(Feilkode feilkode) {
    super(
        feilkode.getBeskrivelse() != null && feilkode.getBeskrivelse().length() > 0
            ? feilkode.getBeskrivelse()
            : message);
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }
}
