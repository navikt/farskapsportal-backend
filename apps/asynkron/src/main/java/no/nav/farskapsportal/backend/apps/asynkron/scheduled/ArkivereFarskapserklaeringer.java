package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.joark.JournalpostApiConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.exception.JournalpostApiConsumerException;
import no.nav.farskapsportal.backend.apps.asynkron.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Slf4j
public class ArkivereFarskapserklaeringer {

  private JournalpostApiConsumer journalpostApiConsumer;
  private PersistenceService persistenceService;
  private SkattConsumer skattConsumer;
  private int intervallMellomForsoek;
  private boolean arkivereIJoark;

  @Scheduled(initialDelay = 60000, fixedDelayString = "${farskapsportal.asynkron.egenskaper.arkiveringsintervall}")
  public void vurdereArkivering() {

    log.info("Ser etter ferdigstilte farskapserklæringer som skal overføres til  Skatt og evnt Joark");
    var farskapserklaeringerTilJoark = persistenceService.henteFarskapserklaeringerHvorFarIkkeBorSammenMedMorOgErSendtTilSkattMenIkkeJoark();
    var farskapserklaeringer = persistenceService.henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt();
    try {
      overfoereTilSkatt(farskapserklaeringer);
    } catch (SkattConsumerException sce) {
      log.error("Overføring til Skatt feilet. Nytt forsøk vil bli gjennomført ved neste overføringsintervall kl {}",
          LocalDateTime.now().plusSeconds(intervallMellomForsoek / 1000));
    }
    // TODO: Fjerne if så snart Joark-integrasjon er klar
    log.warn("Overføring til Joark er skrudd av i påvente av ferdigstilling av integrasjon");
    if (arkivereIJoark) {
      var farskapserklaeringSomSkalOverfoeresTilJoark = farskapserklaeringer.stream()
          .filter(s -> s.getFarBorSammenMedMor() != null && !s.getFarBorSammenMedMor() && s.getJoarkJournalpostId() == null)
          .collect(Collectors.toSet());

      farskapserklaeringSomSkalOverfoeresTilJoark.addAll(farskapserklaeringerTilJoark);
      overfoereTilJoark(farskapserklaeringSomSkalOverfoeresTilJoark);
    }
  }

  private void overfoereTilSkatt(Set<Farskapserklaering> farskapserklaeringer) {
    var fpTekst = farskapserklaeringer.size() == 1 ? "farskapserklæring" : "farskapserklæringer";
    log.info("Fant {} {} som er klar for overføring til skatt.", farskapserklaeringer.size(), fpTekst);
    for (Farskapserklaering fe : farskapserklaeringer) {
      log.debug("Oppdaterer tidspunkt for oversendelse til skatt for farskapserklæring med id {}", fe.getId());
      try {
        var tidspunktForOverfoering = skattConsumer.registrereFarskap(fe);

        fe.setSendtTilSkatt(tidspunktForOverfoering);
        persistenceService.oppdatereFarskapserklaering(fe);
        persistenceService.oppdatereMeldingslogg(fe.getSendtTilSkatt(), fe.getMeldingsidSkatt());
        log.debug("Meldingslogg oppdatert");

      } catch (SkattConsumerException sce) {
        var tidspunktNesteForsoek = LocalDateTime.now().plusSeconds(intervallMellomForsoek / 1000);
        log.error(
            "En feil oppstod i kommunikasjon med Skatt. Farskapserklæring med meldingsidSkatt {} ble ikke overført. Nytt forsøk vil bli igangsatt kl {}",
            fe.getMeldingsidSkatt(), tidspunktNesteForsoek, sce);
        throw sce;
      }
    }
    if (farskapserklaeringer.size() > 0) {
      log.info("Farskapserklæringene ble overført til Skatt uten problemer");
    }
  }

  private void overfoereTilJoark(Set<Farskapserklaering> farskapserklaeringer) {
    var fpTekst = farskapserklaeringer.size() == 1 ? "farskapserklæring" : "farskapserklæringer";
    log.info("Fant {} {} som er klar for overføring til Joark/Dokarkiv.", farskapserklaeringer.size(), fpTekst);
    for (Farskapserklaering fe : farskapserklaeringer) {
      try {
        var opprettJournalpostResponse = journalpostApiConsumer.arkivereFarskapserklaering(fe);
        log.debug("Oppdaterer tidspunkt for oversendelse til Joark for farskapserklæring med id {}", fe.getId());
        fe.setSendtTilJoark(LocalDateTime.now());
        fe.setJoarkJournalpostId(opprettJournalpostResponse.getJournalpostId());
        persistenceService.oppdatereFarskapserklaering(fe);
        log.debug("Meldingslogg oppdatert");
      } catch (JournalpostApiConsumerException jace) {
        var tidspunktNesteForsoek = LocalDateTime.now().plusSeconds(intervallMellomForsoek / 1000);
        log.error(
            "En feil oppstod i kommunikasjon med Joark. Farskapserklæring med meldingsidSkatt {} ble ikke overført. Nytt forsøk vil bli igangsatt kl {}",
            fe.getMeldingsidSkatt(), tidspunktNesteForsoek, jace);
        throw jace;
      }
    }
    if (farskapserklaeringer.size() > 0) {
      log.info("Farskapserklæringene ble overført til Joark uten problemer");
    }
  }
}
