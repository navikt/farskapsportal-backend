package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;

import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;

@Slf4j
public class BrukernotifikasjonConsumer {

  private static final String MELDING_OM_SIGNERT_FARSKAPSERKLAERING =
      "Du har mottatt en signert farskapserklæring som er tilgjengelig for nedlasting i en begrenset tidsperiode.";
  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING =
      "Du har mottatt en farskapserklæring som venter på din signatur.";
  private static final String MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING =
      "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private static final String MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING =
      "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig.";
  private static final String MELDING_OM_MANGLENDE_SIGNERING =
      "Aksjon kreves: Farskapserklæring opprettet den %s for barn med %s er ikke ferdigstilt. Våre systemer mangler informasjon om at far har signert. Far må logge inn på Farskapsportal og forsøke å signere eller oppdatere status på ny. Ta kontakt med NAV ved problemer.";
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE =
      "Far har ikke signert farskapserklæringen innen fristen. Farskapserklæringen er derfor slettet. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";

  private final Beskjedprodusent beskjedprodusent;
  private final Ferdigprodusent ferdigprodusent;
  private final Oppgaveprodusent oppgaveprodusent;
  private final FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  public BrukernotifikasjonConsumer(
      Beskjedprodusent beskjedprodusent,
      Ferdigprodusent ferdigprodusent,
      Oppgaveprodusent oppgaveprodusent,
      FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper)
      throws MalformedURLException {
    this.beskjedprodusent = beskjedprodusent;
    this.ferdigprodusent = ferdigprodusent;
    this.oppgaveprodusent = oppgaveprodusent;
    this.farskapsportalFellesEgenskaper = farskapsportalFellesEgenskaper;
  }

  public void informereForeldreOmTilgjengeligFarskapserklaering(Forelder mor, Forelder far) {
    log.info("Informerer foreldre om ferdigstilt farskapserklæring.");
    SIKKER_LOGG.info(
        "Informerer foreldre (mor: {}, far: {}) om ferdigstilt farskapserklæring.",
        mor.getId(),
        far.getId());
    beskjedprodusent.oppretteBeskjedTilBruker(
        mor,
        MELDING_OM_SIGNERT_FARSKAPSERKLAERING,
        true,
        true,
        UUID.randomUUID().toString(),
        mor.getFoedselsnummer());
    beskjedprodusent.oppretteBeskjedTilBruker(
        far,
        MELDING_OM_SIGNERT_FARSKAPSERKLAERING,
        true,
        true,
        UUID.randomUUID().toString(),
        far.getFoedselsnummer());
  }

  public void varsleForeldreOmManglendeSignering(
      Forelder mor, Forelder far, Barn barn, LocalDate opprettetDato) {
    log.info("Informerer foreldre om ventende farskapserklæring.");
    SIKKER_LOGG.info(
        "Informerer foreldre (mor: {}, far: {}) om ventende farskapserklæring.",
        mor.getId(),
        far.getId());
    var tekstBarn =
        barn.getTermindato() != null
            ? "termindato " + barn.getTermindato().format(DateTimeFormatter.ofPattern("dd.MM.yyy"))
            : "fødselsnummer " + barn.getFoedselsnummer();
    beskjedprodusent.oppretteBeskjedTilBruker(
        mor,
        String.format(
            MELDING_OM_MANGLENDE_SIGNERING,
            opprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            tekstBarn),
        true,
        UUID.randomUUID().toString(),
        mor.getFoedselsnummer());
    beskjedprodusent.oppretteBeskjedTilBruker(
        far,
        String.format(
            MELDING_OM_MANGLENDE_SIGNERING,
            opprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            tekstBarn),
        true,
        UUID.randomUUID().toString(),
        far.getFoedselsnummer());
  }

  public void varsleMorOmUtgaattOppgaveForSignering(Forelder mor) {
    log.info("Sender varsel til mor om utgått signeringsoppgave");
    SIKKER_LOGG.info("Sender varsel til mor med id {} om utgått signeringsoppgave", mor.getId());
    val eventId = UUID.randomUUID().toString();
    beskjedprodusent.oppretteBeskjedTilBruker(
        mor, MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE, true, eventId, mor.getFoedselsnummer());
    log.info("Ekstern melding med eventId: {}, ble sendt til mor", eventId);
  }

  public void varsleOmAvbruttSignering(Forelder mor, Forelder far) {
    log.info("Informerer foreldre om avbrutt signering");
    SIKKER_LOGG.info(
        "Informerer foreldre (mor: {}, far: {}) om avbrutt signering", mor.getId(), far.getId());
    beskjedprodusent.oppretteBeskjedTilBruker(
        mor,
        MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING,
        true,
        UUID.randomUUID().toString(),
        mor.getFoedselsnummer());
    beskjedprodusent.oppretteBeskjedTilBruker(
        far,
        MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING,
        true,
        UUID.randomUUID().toString(),
        far.getFoedselsnummer());
  }

  public void oppretteOppgaveTilFarOmSignering(int idFarskapserklaering, Forelder far) {
    log.info("Oppretter signeringsoppgave med id {}", idFarskapserklaering);
    SIKKER_LOGG.info(
        "Oppretter signeringsoppgave for far {} med id {}", far.getId(), idFarskapserklaering);
    try {
      oppgaveprodusent.oppretteOppgaveForSigneringAvFarskapserklaering(
          idFarskapserklaering,
          far,
          MELDING_OM_VENTENDE_FARSKAPSERKLAERING,
          UUID.randomUUID().toString());
    } catch (InternFeilException internFeilException) {
      log.error(
          "En feil inntraff ved opprettelse av oppgave til far for farskapserklæring med id {}",
          idFarskapserklaering);
    }
  }

  public void sletteFarsSigneringsoppgave(String eventId, Forelder far) {
    log.info("Sletter signeringsoppgave med eventId {}", eventId);
    SIKKER_LOGG.info("Sletter signeringsoppgave for far {} med eventId {}", far.getId(), eventId);
    try {
      ferdigprodusent.ferdigstilleFarsSigneringsoppgave(far, eventId);
    } catch (InternFeilException internFeilException) {
      log.error(
          "En feil oppstod ved sending av ferdigmelding for oppgave med eventId {}.", eventId);
    }
  }
}
