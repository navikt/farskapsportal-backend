package no.nav.farskapsportal.api;

public enum Feilkode {

  BARN_MANGLER_RELASJON_TIL_MOR("Oppgitt barn mangler relasjon til mor"), BARN_HAR_FLERE_ERLAERINGER(
      "Feil i datagrunnlag. Barnet er involvert i mer enn én farskapserklæering"), ERKLAERING_EKSISTERER_UFOEDT(
      "Det eksisterer allerede en farskapserklæring med samme foreldrepar med termindato innenfor gyldig intervall"),
  ERKLAERING_EKSISTERER_BARN(
      "Det eksisterer allrede en farskapserklæring for oppgitt barn"), FEIL_ROLLE_OPPRETTE(
      "Personen har ikke rettigheter til å opprette farskapserklæring"), IKKE_MYNDIG("Personen er ikke myndig"), INGEN_NYFOEDTE_UTEN_FAR(
      "Mor er ikke registrert med noen nyfødte barn uten oppgitt far"), NYFODT_ER_FOR_GAMMEL(
      "Gyldighetsperioden for å erklære farskap er utløpt for oppgitt barn"), MEDMOR_ELLER_UKJENT(
      "Medmor eller person med ukjent roll kan ikke benytte løsningen"), MOR_HAR_FLERE_ERKLAERINGER(
      "Feil i datagrunnlag. Mor har mer enn én pågående farskapserklæring."), MOR_SIVILSTAND_GIFT(
      "Mor kan ikke opprette farskapserklæring dersom hun er gift"), MOR_SIVILSTAND_REGISTRERT_PARTNER(
      "Mor kan ikke opprette farskapserklæring dersom hun er registrert partner"), MOR_SIVILSTAND_UOPPGITT(
      "Mor kan ikke opprette farskapserklæring dersom hun har sivilstand uoppgitt"), OPPRETTE_SIGNERINGSJOBB(
      "Feil oppstod ved opprettelse av signeringsjobb mot Posten");

  private final String beskrivelse;

  Feilkode(String beskrivelse) {
    this.beskrivelse = beskrivelse;
  }

  public String getBeskrivelse() {
    return this.beskrivelse;
  }
}
