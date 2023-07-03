package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;
import static no.nav.farskapsportal.backend.libs.felles.util.Utils.getMeldingsidSkatt;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Builder
public class ArkivereFarskapserklaeringer {

  private final DifiESignaturConsumer difiESignaturConsumer;
  private PersistenceService persistenceService;
  private SkattConsumer skattConsumer;
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
          "Oppdaterer tidspunkt for oversendelse til skatt for farskapserklæring med id {}",
          farskapserklaeringsid);
      var farskapserklaering =
          persistenceService.henteFarskapserklaeringForId(farskapserklaeringsid);

      var status =
          difiESignaturConsumer.henteStatus(
              farskapserklaering.getDokument().getStatusQueryToken(),
              farskapserklaering.getDokument().getJobbref(),
              tilUri(farskapserklaering.getDokument().getStatusUrl()));
      var lagretPades = farskapserklaering.getDokument().getDokumentinnhold().getInnhold();
      var xadesMor = farskapserklaering.getDokument().getSigneringsinformasjonMor().getXadesXml();
      var xadesFar = farskapserklaering.getDokument().getSigneringsinformasjonFar().getXadesXml();

      if (lagretPades == null || xadesMor == null || xadesFar == null) {
        log.info(
            "Henter oppdaterte signeringsdokumenter fra esigneringstjenesten for farskapserklaering med id {}",
            farskapserklaering.getId());
        var pades = difiESignaturConsumer.henteSignertDokument(status.getPadeslenke());
        if (pades != null) {
          farskapserklaering
              .getDokument()
              .setDokumentinnhold(Dokumentinnhold.builder().innhold(pades).build());
        }
        henteOgLagreXadesXml(farskapserklaering, status.getSignaturer());
      }

      if (farskapserklaering.getMeldingsidSkatt() == null) {
        farskapserklaering.setMeldingsidSkatt(getMeldingsidSkatt(farskapserklaering));
        try {
          persistenceService.oppdatereFarskapserklaering(farskapserklaering);
        } catch (Exception e) {
          e.printStackTrace();
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

  private void henteOgLagreXadesXml(
      Farskapserklaering farskapserklaering, List<SignaturDto> signaturer) {
    for (SignaturDto signatur : signaturer) {
      var xades = difiESignaturConsumer.henteXadesXml(signatur.getXadeslenke());
      if (signatur.getSignatureier().equals(farskapserklaering.getMor().getFoedselsnummer())
          && xades != null) {
        farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml(xades);
      } else if (signatur.getSignatureier().equals(farskapserklaering.getFar().getFoedselsnummer())
          && xades != null) {
        farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml(xades);
      } else {
        log.error(
            "Personer i signeringsoppdrag stemmer ikke med foreldrene i farskapserklæring med id {}",
            farskapserklaering.getId());
        SIKKER_LOGG.error(
            "Person i signeringsoppdrag (personident: {}), er forskjellig fra foreldrene i farskapserklæring med id {}",
            signatur.getSignatureier(),
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
