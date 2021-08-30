package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.net.URL;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class BrukernotifikasjonConsumer {

  private static final String MELDING_OM_SIGNERT_FARSKAPSERKLAERING = "Du har en signert farskapserklæring er tilgjengelig for nedlasting i en begrenset tidsperiode fra farskapsportalen:";
  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING = "Du har mottatt en farskapserklæring som venter på din signatur.";
  private static final String MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Trykk her for å opprette ny farskapserklæring.";
  private static final String MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan evntuelt opprette ny hvis ønskelig.";
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Trykk her for å opprette ny farskapserklæring.";


  private final URL farskapsportalUrl;
  private final Beskjedprodusent beskjedprodusent;
  private final Ferdigprodusent ferdigprodusent;
  private final Oppgaveprodusent oppgaveprodusent;

  public void informereForeldreOmTilgjengeligFarskapserklaering(String foedselsnummerMor, String foedselsnummerFar) {
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerMor, MELDING_OM_SIGNERT_FARSKAPSERKLAERING, true, farskapsportalUrl);
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerFar, MELDING_OM_SIGNERT_FARSKAPSERKLAERING, true, farskapsportalUrl);
  }

  public void varsleMorOmUtgaattOppgaveForSignering(String foedselsnummerMor) {
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerMor, MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE, true, farskapsportalUrl);
  }

  public void varsleOmAvbruttSignering(String foedselsnummerMor, String foedselsnummerFar) {
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerMor, MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING, true, farskapsportalUrl);
    beskjedprodusent.oppretteBeskjedTilBruker(foedselsnummerFar, MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING, true, farskapsportalUrl);
  }

  public void oppretteOppgaveTilFarOmSignering(int idFarskapserklaering, String foedselsnummerFar) {
    log.info("Oppretter oppgave om signering til far i farskapserklæring med id {}", idFarskapserklaering);
    oppgaveprodusent
        .oppretteOppgaveForSigneringAvFarskapserklaering(Integer.toString(idFarskapserklaering), foedselsnummerFar,
            MELDING_OM_VENTENDE_FARSKAPSERKLAERING, true);
  }

  public void sletteFarsSigneringsoppgave(int idFarskapserklaering, String foedselsnummerFar) {
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(Integer.toString(idFarskapserklaering), foedselsnummerFar);
  }
}
