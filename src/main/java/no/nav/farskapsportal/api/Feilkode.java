package no.nav.farskapsportal.api;

public enum Feilkode {

  BARN_FOEDT_UTENFOR_NORGE("Oppgitt nyfødt barn er ikke født i Norge"),
  BARN_MANGLER_RELASJON_TIL_MOR("Oppgitt barn mangler relasjon til mor"),
  BARN_HAR_FLERE_ERLAERINGER("Feil i datagrunnlag. Barnet er involvert i mer enn én farskapserklæering"),
  BARN_HAR_IKKE_FNUMMER("Barnet er registrert med dnummer i PDL"),
  BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE("Opprettelse av brukernotifikasjonsoppgave feilet!"),
  DOKUMENT_MANGLER_INNOHLD("Farskapserklæringsdokumentet er uten innhold"),
  ERKLAERING_EKSISTERER_BARN("Det eksisterer allrede en farskapserklæring for oppgitt barn"),
  ERKLAERING_EKSISTERER_MOR("Mor har eksisterende farskapserklæring. Kan ikke opprette ny erklæring for ufødt barn."),
  ESIGNERING_MANGLENDE_DATA("Manglende data retunert fra status-kall mot esigneringstjenesten"),
  ESIGNERING_UKJENT_TOKEN("Ingen treff på oppgitt status query token hos esigneringstjenesten"),
  ESIGNERING_SIGNATUREIER_NULL("Signatureier er null i respons fra esigneringsløsningen!"),
  ESIGNERING_STATUS_FEILET("Signeringsoppdraget har status feilet hos Posten, og signering kan ikke gjennomføres."),
  ESIGNERING_REDIRECTURL_UKJENT("Redirecturl for ukjent part mottatt fra esigneringsløsningen!"),
  FARSKAPSERKLAERING_MANGLER_DATA("Farskapserklæringen mangler påkrevde data"),
  FARSKAPSERKLAERING_MANGLER_SIGNATUR("Farskapserklæringen er ikke signert av begge foreldrene"),
  FARSKAPSERKLAERING_MANGLER_SIGNATUR_MOR("Mor har ikke signert farskapserklæringen"),
  FEILFORMATERT_URL_UNDERTEGNERURL("URL for å hente ny redirect-url er feilformattert"),
  FANT_IKKE_FARSKAPSERKLAERING("Oppgitt farskapserklæring ble ikke funnet i databasen"),
  FAR_HAR_ALLEREDE_SIGNERT("Far har allerede signert farskapserklæringen"),
  FAR_HAR_IKKE_FNUMMER("Far er ikke registrert med fødselsnummer i PDL"),
  FEIL_ROLLE("Pålogget person kan verken opptre som mor eller far i løsningen!"),
  FEIL_ROLLE_FAR("Personen har ikke riktig rolle for å kunne opptre som far i løsningen"),
  FEIL_ROLLE_OPPRETTE("Personen har ikke rettigheter til å opprette farskapserklæring"),
  FOEDSELNUMMER_MANGLER_FAR("Fødselsnummer mangler for oppgitt far!"),
  FORELDER_HAR_VERGE("Forelder er registrert med verge i Folkeregisteret, og kan derfor ikke bruke løsningen."),
  IKKE_MYNDIG("Personen er ikke myndig"),
  INGEN_NYFOEDTE_UTEN_FAR("Mor er ikke registrert med noen nyfødte barn uten oppgitt far"),
  INGEN_TREFF_PAA_TOKEN("Ingen treff på oppgitt token i personens påbegyntes farskapserklaeringer"),
  INTERN_FEIL_OPPDATERING_AV_ERKLAERING("En feil oppstod ved oppdatering av farskapserklæring"),
  NYFODT_ER_FOR_GAMMEL("Gyldighetsperioden for å erklære farskap er utløpt for oppgitt barn"),
  MEDMOR_ELLER_UKJENT("Medmor eller person med ukjent roll kan ikke benytte løsningen"),
  FORSKJELLIGE_FEDRE("Mor kan ikke opprette farskapserklæringer med forskjellige fedre for samme kull nyfødte"),
  KONTROLLERE_FAR_NAVN_MANGLER("Navn på far mangler!"),
  MAKS_ANTALL_FORSOEK("Mor har brukt opp antall mulige forsøk på å komme frem til riktig kombinasjon av fars fødselsnummer og navn"),
  MOR_HAR_ALLEREDE_SIGNERT("Mor har allerede signert farskapserklæringen"),
  MOR_HAR_IKKE_FNUMMER("Mor er registrert med fødselsnummer i PDL"),
  MOR_IKKE_NORSK_BOSTEDSADRESSE("Mor er ikke registrert med norsk bostedsadresse."),
  MOR_OG_FAR_SAMME_PERSON("Mor kan ikke oppgi seg selv som far."),
  MOR_SIVILSTAND_GIFT("Mor kan ikke opprette farskapserklæring dersom hun er gift"),
  MOR_SIVILSTAND_REGISTRERT_PARTNER("Mor kan ikke opprette farskapserklæring dersom hun er registrert partner"),
  MOR_SIVILSTAND_UOPPGITT("Mor kan ikke opprette farskapserklæring dersom hun har sivilstand uoppgitt"),
  NAVN_STEMMER_IKKE_MED_REGISTER("Oppgitt navn til far stemmer ikke med fars navn i Folkeregisteret"),
  OPPDATERING_IKKE_MULIG("Ikke mulig å oppdatere oppgitt ressurs"),
  OPPRETTE_SIGNERINGSJOBB("Feil oppstod ved opprettelse av signeringsjobb mot Posten"),
  OPPRETTE_PDF_FEILET("Opprettelse av PDF feilet"),
  PADESURL_FEILFORMATERT("PAdES-url mottatt fre Esigneringsløsningen var ikke formatert riktig"),
  PERSON_ER_DOED("Død person kan ikke opptre som forelder i løsningen"),
  PDL_FEIL("Respons fra PDL inneholder feil"),
  PDL_BOSTEDSADRESSE_MANGLER("Respons fra PDL inneholdt ikke informasjon om personens bostedsadresse"),
  PDL_FOEDSELSDATO_TEKNISK_FEIL("Feil inntraff ved henting av fødselsdato fra PDL for person"),
  PDL_FOEDSELSDATO_MANGLER("Respons fra PDL inneholdt ingen informasjon om personens foedselsdato..."),
  PDL_FOLKEREGISTERIDENTIFIKATOR_IKKE_FUNNET("Respons fra PDL inneholdt ingen informasjon om personens folkeregisteridentifikator"),
  PDL_KJOENN_LAVESTE_GYLDIGHETSTIDSPUNKT("Feil ved henting av laveste gyldighetstidspunkt for kjønnshistorikk"),
  PDL_KJOENN_INGEN_INFO("Respons fra PDL inneholdt ingen informasjon om kjønn..."),
  PDL_KJOENN_ORIGINALT("Feil ved henting av originalt kjønn fra PDL"),
  PDL_NAVN_IKKE_FUNNET("Fant ikke personens navn i PDL"),
  PDL_PERSON_IKKE_FUNNET("Fant ikke person i PDL"),
  PDL_SIVILSTAND_IKKE_FUNNET("Fant ikke informasjon om personens sivilstand i PDL"),
  PERSON_HAR_ALLEREDE_SIGNERT("Personen har allerede signert farskapserklæringen, og kan derfor ikke bestile ny redirect-url "),
  PERSON_IKKE_PART_I_FARSKAPSERKLAERING("Pålogget person er ikke forelder i oppgitt farskapserklaering"),
  PERSON_HAR_INGEN_VENTENDE_FARSKAPSERKLAERINGER("Paalogget person har ingen farskapserklæringer som venter på signering"),
  SKATT_OVERFOERING_FEILET("Overføring av farskapsmelding til Skatt feilet"),
  SKATT_MELDINGSFORMAT("Feil oppstod ved opprettelse av farskapsmelding til Skatt"),
  TERMINDATO_UGYLDIG("Oppgitt termindato er ikke innenfor godkjent intervall"),
  UKJENT_SIGNERINGSSTATUS("Ukjent signeringsstatus mottatt fra esigneringstjenesten"),
  XADESURL_FEILFORMATERT("XAdES-url mottatt fre Esigneringsløsningen var ikke formatert riktig"),
  XADES_MOR_UTEN_INNHOLD("Mors XAdES-dokument er uten innhold!"),
  XADES_FAR_UTEN_INNHOLD("Fars XAdES-dokument er uten innhold!");

  private final String beskrivelse;

  Feilkode(String beskrivelse) {
    this.beskrivelse = beskrivelse;
  }

  public String getBeskrivelse() {
    return this.beskrivelse;
  }
}
