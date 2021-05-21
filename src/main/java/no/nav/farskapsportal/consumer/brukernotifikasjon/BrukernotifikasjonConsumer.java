package no.nav.farskapsportal.consumer.brukernotifikasjon;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class BrukernotifikasjonConsumer {

  private static final String MELDING_OM_SIGNERT_FARSKAPSERKLAERING = "En signert farskapserklæring er tilgjengelig i din innboks";
  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING = "Du har mottatt en farskapserklæring som venter på din signatur.";

  private final Beskjedprodusent beskjedprodusent;
  private final Ferdigprodusent ferdigprodusent;
  private final Oppgaveprodusent oppgaveprodusent;

  public void informereForeldreOmTilgjengeligFarskapserklaering(String foedselsnummerMor, String foedselsnummerFar) {
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerMor, MELDING_OM_SIGNERT_FARSKAPSERKLAERING, true);
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerFar, MELDING_OM_SIGNERT_FARSKAPSERKLAERING, true);
  }

  public void oppretteOppgaveTilFarOmSignering(String idFarskapserklaering, String foedselsnummerFar) {
    oppgaveprodusent
        .oppretteOppgaveForSigneringAvFarskapserklaering(idFarskapserklaering, foedselsnummerFar, MELDING_OM_VENTENDE_FARSKAPSERKLAERING, false);
  }

  public void varsleFarOmSigneringsoppgave(String foedselsnummerFar) {
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerFar, MELDING_OM_VENTENDE_FARSKAPSERKLAERING, true);
  }

  public void sletteFarsSigneringsoppgave(String idFarskapserklaering, String foedselsnummerFar) {
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(idFarskapserklaering, foedselsnummerFar);
  }
}
