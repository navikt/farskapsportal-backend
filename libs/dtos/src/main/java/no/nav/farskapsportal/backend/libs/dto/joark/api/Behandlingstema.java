package no.nav.farskapsportal.backend.libs.dto.joark.api;

public enum Behandlingstema {

  BIDRAG_INKLUSIV_FARSKAP("ab0322");

  private final String kode;

  Behandlingstema(String kode) {
    this.kode = kode;
  }

  public String getKode()  {
    return this.kode;
  }


}