package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.net.URL;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class BrukernotifikasjonConsumer {

  private static final String MELDING_OM_SIGNERT_FARSKAPSERKLAERING = "En signert farskapserklæring er tilgjengelig i din innboks";
  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING = "Du har mottatt en farskapserklæring som venter på din signatur.";

  private final URL farskapsportalUrl;
  private final Beskjedprodusent beskjedprodusent;
  private final Ferdigprodusent ferdigprodusent;
  private final Oppgaveprodusent oppgaveprodusent;

  public void informereForeldreOmTilgjengeligFarskapserklaering(String foedselsnummerMor, String foedselsnummerFar) {
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerMor, MELDING_OM_SIGNERT_FARSKAPSERKLAERING, true, null);
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerFar, MELDING_OM_SIGNERT_FARSKAPSERKLAERING, true, null);
  }

  public void oppretteOppgaveTilFarOmSignering(int idFarskapserklaering, String foedselsnummerFar) {
    oppgaveprodusent
        .oppretteOppgaveForSigneringAvFarskapserklaering(Integer.toString(idFarskapserklaering), foedselsnummerFar, MELDING_OM_VENTENDE_FARSKAPSERKLAERING, false);
  }

  // Forsinket ekstern varsling - kalles av skedulert jobb
  public void varsleFarOmSigneringsoppgave(String foedselsnummerFar) {
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerFar, MELDING_OM_VENTENDE_FARSKAPSERKLAERING, true, farskapsportalUrl);
  }

  public void sletteFarsSigneringsoppgave(int idFarskapserklaering, String foedselsnummerFar) {
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(Integer.toString(idFarskapserklaering), foedselsnummerFar);
  }
}
