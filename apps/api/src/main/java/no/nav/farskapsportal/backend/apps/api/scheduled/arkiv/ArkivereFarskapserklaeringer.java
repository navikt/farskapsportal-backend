package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.api.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.apps.api.service.FarskapsportalService;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Builder
public class ArkivereFarskapserklaeringer {

  private final BucketConsumer bucketConsumer;
  private final FarskapsportalService farskapsportalService;
  private final PersistenceService persistenceService;
  private final SkattConsumer skattConsumer;
  private int intervallMellomForsoek;
  private int maksAntallFeilPaaRad;

  @Transactional
  @SchedulerLock(name = "arkivere", lockAtLeastFor = "PT5M", lockAtMostFor = "PT120M")
  @Scheduled(
      initialDelayString = "${farskapsportal.asynkron.egenskaper.arkiv.arkiveringsforsinkelse}",
      fixedDelayString = "${farskapsportal.asynkron.egenskaper.arkiv.arkiveringsintervall}")
  public void vurdereArkivering() {

    log.info("Ser etter ferdigstilte farskapserklæringer som skal overføres til Skatt.");
    var farskapserklaeringer =
        persistenceService.henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt();
    try {
      overfoereTilSkatt(farskapserklaeringer);
    } catch (SkattConsumerException sce) {
      log.error(
          "Overføring til Skatt feilet. Nytt forsøk vil bli gjennomført ved neste overføringsintervall kl {}",
          LocalDateTime.now().plusSeconds(intervallMellomForsoek / 1000));
    }
  }

  private void overfoereTilSkatt(Set<Integer> farskapserklaeringsider) {
    var fpTekst = farskapserklaeringsider.size() == 1 ? "farskapserklæring" : "farskapserklæringer";
    var antallFeilPaaRad = 0;
    log.info(
        "Fant {} {} som er klar for overføring til skatt.",
        farskapserklaeringsider.size(),
        fpTekst);
    for (Integer farskapserklaeringsid : farskapserklaeringsider) {
      log.debug(
          "Setter tidspunkt for oversendelse til skatt for farskapserklæring med id {}",
          farskapserklaeringsid);
      var farskapserklaering =
          persistenceService.henteFarskapserklaeringForId(farskapserklaeringsid);

      var blobIdXadesFar =
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getBlobIdGcp();

      if (blobIdXadesFar == null) {
        log.info(
            "Henter oppdaterte signeringsdokumenter fra esigneringstjenesten for farskapserklaering med id {}",
            farskapserklaering.getId());

        farskapsportalService.henteOgLagrePades(farskapserklaering);

        var blobIdXadesMor =
            farskapserklaering.getDokument().getSigneringsinformasjonMor().getBlobIdGcp();
        if (blobIdXadesMor == null || blobIdXadesFar == null) {
          farskapsportalService.henteOgLagreXadesXml(farskapserklaering);
        }
      }

      try {
        var tidspunktForOverfoering = skattConsumer.registrereFarskap(farskapserklaering);
        farskapserklaering.setSendtTilSkatt(tidspunktForOverfoering);
        persistenceService.oppdatereFarskapserklaering(farskapserklaering);
        persistenceService.oppdatereMeldingslogg(
            farskapserklaering.getSendtTilSkatt(), farskapserklaering.getMeldingsidSkatt());
        antallFeilPaaRad = 0;

        log.debug("Meldingslogg oppdatert");

      } catch (SkattConsumerException sce) {
        var tidspunktNesteForsoek = LocalDateTime.now().plusSeconds(intervallMellomForsoek / 1000);
        log.error(
            "En feil oppstod i kommunikasjon med Skatt. Farskapserklæring med meldingsidSkatt {} ble ikke overført. Nytt forsøk vil bli igangsatt kl {}",
            farskapserklaering.getMeldingsidSkatt(),
            tidspunktNesteForsoek,
            sce);
        antallFeilPaaRad++;
        if (maksAntallFeilPaaRad <= antallFeilPaaRad) {
          throw sce;
        }
      }
    }
    if (farskapserklaeringsider.size() > 0) {
      log.info("Farskapserklæringene ble overført til Skatt uten problemer");
    }
  }
}
