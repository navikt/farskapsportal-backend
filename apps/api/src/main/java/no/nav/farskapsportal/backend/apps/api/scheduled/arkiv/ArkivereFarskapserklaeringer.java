package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.api.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Builder
@Slf4j
public class ArkivereFarskapserklaeringer {

  private PersistenceService persistenceService;
  private SkattConsumer skattConsumer;
  private int intervallMellomForsoek;

  @Scheduled(initialDelayString = "${farskapsportal.asynkron.egenskaper.arkiv.arkiveringsforsinkelse}", fixedDelayString = "${farskapsportal.asynkron.egenskaper.arkiv.arkiveringsintervall}")
  public void vurdereArkivering() {

    log.info("Ser etter ferdigstilte farskapserklæringer som skal overføres til Skatt.");
    var farskapserklaeringer = persistenceService.henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt();
    try {
      overfoereTilSkatt(farskapserklaeringer);
    } catch (SkattConsumerException sce) {
      log.error("Overføring til Skatt feilet. Nytt forsøk vil bli gjennomført ved neste overføringsintervall kl {}",
          LocalDateTime.now().plusSeconds(intervallMellomForsoek / 1000));
    }
  }

  private void overfoereTilSkatt(Set<Integer> farskapserklaeringsider) {
    var fpTekst = farskapserklaeringsider.size() == 1 ? "farskapserklæring" : "farskapserklæringer";
    log.info("Fant {} {} som er klar for overføring til skatt.", farskapserklaeringsider.size(), fpTekst);
    for (Integer farskapserklaeringsid : farskapserklaeringsider) {
      log.debug("Oppdaterer tidspunkt for oversendelse til skatt for farskapserklæring med id {}", farskapserklaeringsid);
      var farskapserklaering = persistenceService.henteFarskapserklaeringForId(farskapserklaeringsid);
      try {
        var tidspunktForOverfoering = skattConsumer.registrereFarskap(farskapserklaering);
        farskapserklaering.setSendtTilSkatt(tidspunktForOverfoering);
        persistenceService.oppdatereFarskapserklaering(farskapserklaering);
        persistenceService.oppdatereMeldingslogg(farskapserklaering.getSendtTilSkatt(), farskapserklaering.getMeldingsidSkatt());
        log.debug("Meldingslogg oppdatert");

      } catch (SkattConsumerException sce) {
        var tidspunktNesteForsoek = LocalDateTime.now().plusSeconds(intervallMellomForsoek / 1000);
        log.error(
            "En feil oppstod i kommunikasjon med Skatt. Farskapserklæring med meldingsidSkatt {} ble ikke overført. Nytt forsøk vil bli igangsatt kl {}",
            farskapserklaering.getMeldingsidSkatt(), tidspunktNesteForsoek, sce);
        throw sce;
      }
    }
    if (farskapserklaeringsider.size() > 0) {
      log.info("Farskapserklæringene ble overført til Skatt uten problemer");
    }
  }
}
