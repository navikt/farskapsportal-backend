package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;
import static no.nav.farskapsportal.backend.libs.felles.util.Utils.getMeldingsidSkatt;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.api.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
import no.nav.farskapsportal.backend.libs.felles.exception.MappingException;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Builder
@Slf4j
public class ArkivereFarskapserklaeringer {

  private final DifiESignaturConsumer difiESignaturConsumer;
  private PersistenceService persistenceService;
  private SkattConsumer skattConsumer;
  private int intervallMellomForsoek;

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
    log.info(
        "Fant {} {} som er klar for overføring til skatt.",
        farskapserklaeringsider.size(),
        fpTekst);
    for (Integer farskapserklaeringsid : farskapserklaeringsider) {
      log.debug(
          "Oppdaterer tidspunkt for oversendelse til skatt for farskapserklæring med id {}",
          farskapserklaeringsid);
      var farskapserklaering =
          persistenceService.henteFarskapserklaeringForId(farskapserklaeringsid);

      if (farskapserklaering.getMeldingsidSkatt() == null) {
        farskapserklaering.setMeldingsidSkatt(getMeldingsidSkatt(farskapserklaering));
      }

      var status =
          difiESignaturConsumer.henteStatus(
              farskapserklaering.getDokument().getStatusQueryToken(),
              farskapserklaering.getDokument().getJobbref(),
              tilUri(farskapserklaering.getDokument().getStatusUrl()));

      var pades = difiESignaturConsumer.henteSignertDokument(status.getPadeslenke());
      farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold(pades).build());

      henteOgLagreXadesXml(farskapserklaering, status.getSignaturer());

      try {
        var tidspunktForOverfoering = skattConsumer.registrereFarskap(farskapserklaering);
        farskapserklaering.setSendtTilSkatt(tidspunktForOverfoering);
        persistenceService.oppdatereFarskapserklaering(farskapserklaering);
        persistenceService.oppdatereMeldingslogg(
            farskapserklaering.getSendtTilSkatt(), farskapserklaering.getMeldingsidSkatt());
        log.debug("Meldingslogg oppdatert");

      } catch (SkattConsumerException sce) {
        var tidspunktNesteForsoek = LocalDateTime.now().plusSeconds(intervallMellomForsoek / 1000);
        log.error(
            "En feil oppstod i kommunikasjon med Skatt. Farskapserklæring med meldingsidSkatt {} ble ikke overført. Nytt forsøk vil bli igangsatt kl {}",
            farskapserklaering.getMeldingsidSkatt(),
            tidspunktNesteForsoek,
            sce);
        throw sce;
      }
    }
    if (farskapserklaeringsider.size() > 0) {
      log.info("Farskapserklæringene ble overført til Skatt uten problemer");
    }
  }

  private void henteOgLagreXadesXml(
      Farskapserklaering farskapserklaering, List<SignaturDto> signaturer) {
    for (SignaturDto signatur : signaturer) {
      var xades = difiESignaturConsumer.henteXadesXml(signatur.getXadeslenke());
      if (signatur.getSignatureier().equals(farskapserklaering.getMor().getFoedselsnummer())) {
        farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml(xades);
      } else if (signatur
          .getSignatureier()
          .equals(farskapserklaering.getFar().getFoedselsnummer())) {
        farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml(xades);
      } else {
        log.error(
            "Personer i signeringsoppdrag stemmer ikke med foreldrene i farskapserklæring med id {}",
            farskapserklaering.getId());
        SIKKER_LOGG.error(
            "Personer i signeringsoppdrag ({} og {}), er forskjellig fra foreldrene i farskapserklæring med id {}",
            farskapserklaering.getId());
        throw new InternFeilException(Feilkode.FARSKAPSERKLAERING_HAR_INKONSISTENTE_DATA);
      }
    }
  }

  private URI tilUri(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException urise) {
      throw new MappingException("Lagret status-URL har feil format", urise);
    }
  }
}
