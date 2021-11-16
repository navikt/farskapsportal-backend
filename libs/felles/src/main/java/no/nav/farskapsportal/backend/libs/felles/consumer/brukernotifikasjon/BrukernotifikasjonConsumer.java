package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
import no.nav.farskapsportal.backend.libs.entity.Forelder;

@Slf4j
public class BrukernotifikasjonConsumer {

  private static final String MELDING_OM_SIGNERT_FARSKAPSERKLAERING = "Du har en signert farskapserklæring er tilgjengelig for nedlasting i en begrenset tidsperiode fra farskapsportalen:";
  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING = "Du har mottatt en farskapserklæring som venter på din signatur.";
  private static final String MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private static final String MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig.";
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Farskapserklæringen er derfor slettet. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private final Beskjedprodusent beskjedprodusent;
  private final Ferdigprodusent ferdigprodusent;
  private final Oppgaveprodusent oppgaveprodusent;
  private final String navnFarskapsportalSystembruker;

  public BrukernotifikasjonConsumer(Beskjedprodusent beskjedprodusent, Ferdigprodusent ferdigprodusent, Oppgaveprodusent oppgaveprodusent, String navnFarskapsportalSystembruker)
      throws MalformedURLException {
    this.beskjedprodusent = beskjedprodusent;
    this.ferdigprodusent = ferdigprodusent;
    this.oppgaveprodusent = oppgaveprodusent;
    this.navnFarskapsportalSystembruker = navnFarskapsportalSystembruker;
  }

  public void informereForeldreOmTilgjengeligFarskapserklaering(Forelder mor, Forelder far) {
    log.info("Informerer foreldre (mor: {}, far: {}) om ferdigstilt farskapserklæring.", mor.getId(), far.getId());
    beskjedprodusent.oppretteBeskjedTilBruker(mor, MELDING_OM_SIGNERT_FARSKAPSERKLAERING, true, oppretteNokkel());
    beskjedprodusent.oppretteBeskjedTilBruker(far, MELDING_OM_SIGNERT_FARSKAPSERKLAERING, true, oppretteNokkel());
  }

  public void varsleMorOmUtgaattOppgaveForSignering(Forelder mor) {
    log.info("Sender varsel til mor om utgått signeringsoppgave");
    var noekkel = oppretteNokkel();
    beskjedprodusent.oppretteBeskjedTilBruker(mor, MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE, true, noekkel);
    log.info("Ekstern melding med eventId: {}, ble sendt til mor", noekkel.getEventId());
  }

  public void varsleOmAvbruttSignering(Forelder mor, Forelder far) {
    log.info("Varsler brukere om avbrutt signering");
    beskjedprodusent.oppretteBeskjedTilBruker(mor, MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING, true, oppretteNokkel());
    beskjedprodusent.oppretteBeskjedTilBruker(far, MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING, true, oppretteNokkel());
  }

  public void oppretteOppgaveTilFarOmSignering(int idFarskapserklaering, Forelder far) {
    try {
      oppgaveprodusent
          .oppretteOppgaveForSigneringAvFarskapserklaering(idFarskapserklaering, far,
              MELDING_OM_VENTENDE_FARSKAPSERKLAERING, true);
    } catch (InternFeilException internFeilException) {
      log.error("En feil inntraff ved opprettelse av oppgave til far for farskapserklæring med id {}", idFarskapserklaering);
    }
  }

  public void sletteFarsSigneringsoppgave(String eventId, Forelder far) {
    log.info("Sletter signeringsoppgave med eventId {}", eventId);
    try {
      ferdigprodusent.ferdigstilleFarsSigneringsoppgave(far, oppretteNokkel(eventId));
    } catch (InternFeilException internFeilException) {
      log.error("En feil oppstod ved sending av ferdigmelding for oppgave med eventId {}.");
    }
  }

  private Nokkel oppretteNokkel() {
    var unikEventid = UUID.randomUUID().toString();
    return oppretteNokkel(unikEventid);
  }

  private Nokkel oppretteNokkel(String eventId) {
    return new NokkelBuilder().withSystembruker(navnFarskapsportalSystembruker).withEventId(eventId).build();
  }
}
