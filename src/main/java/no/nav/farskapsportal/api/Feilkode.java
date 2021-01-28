package no.nav.farskapsportal.api;

public enum Feilkode {
  BARN_MANGLER_RELASJON_TIL_MOR("Oppgitt barn mangler relasjon til mor"),
  ERKLAERING_EKSISTERER_UFOEDT("Det eksisterer allerede en farskapserklæring med samme foreldrepar med termindato innenfor gyldig intervall"),
  ERKLAERING_EKSISTERER("Det eksisterer allrede en farskapserklæring for oppgitt barn"),
  INGEN_NYFOEDTE_UTEN_FAR("Mor er ikke registrert med noen nyfødte barn uten oppgitt far");

  private final String beskrivelse;

  Feilkode(String beskrivelse) {this.beskrivelse = beskrivelse; }

  public String getBeskrivelse() {return this.beskrivelse;}
}
