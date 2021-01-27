package no.nav.farskapsportal.api;

public enum Feilkode {
  ERKLAERING_EKSISTERER_UFOEDT("Det eksisterer allerede en farskapserklæring med samme foreldrepar med termindato innenfor gyldig intervall"),
  ERKLAERING_EKSISTERER("Det eksisterer allrede en farskapserklæring for oppgitt barn");

  private final String beskrivelse;

  Feilkode(String beskrivelse) {this.beskrivelse = beskrivelse; }

  public String getBeskrivelse() {return this.beskrivelse;}
}
