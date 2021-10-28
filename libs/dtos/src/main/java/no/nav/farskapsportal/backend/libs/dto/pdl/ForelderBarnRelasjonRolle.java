package no.nav.farskapsportal.backend.libs.dto.pdl;

public enum ForelderBarnRelasjonRolle {
  BARN,
  MOR,
  FAR,
  MEDMOR;

  public enum Sivilstandtype {
    GIFT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    UGIFT,
    UOPPGITT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    GJENLEVENDE_PARTNER

  }
}
