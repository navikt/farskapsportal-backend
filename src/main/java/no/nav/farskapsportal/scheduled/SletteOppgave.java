package no.nav.farskapsportal.scheduled;

import java.time.LocalDate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Builder
@Slf4j
public class SletteOppgave {

  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  // Kjøres hver morgen kl 06:00
  @Scheduled(cron = "0 00 6 * * ?")
  public void sletteUtloepteSigneringsoppgaver() {
    var farskapserklaeringerSomVenterPaaFar = persistenceService.henteFarskapserklaeringerSomVenterPaaFarsSignatur();

    for (Farskapserklaering farskapserklaering : farskapserklaeringerSomVenterPaaFar) {
      if (farskapsportalEgenskaper.getBrukernotifikasjon().isSkruddPaa() && farskapserklaering.getDokument().getSigneringsinformasjonMor()
          .getSigneringstidspunkt().toLocalDate()
          .isBefore(LocalDate.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager() - 1))) {
        log.info("Sletter utgått signeringsoppgave for farskapserklæring med id {}", farskapserklaering.getId());
        brukernotifikasjonConsumer
            .sletteFarsSigneringsoppgave(farskapserklaering.getId(), farskapserklaering.getFar().getFoedselsnummer());
        sletteFarskapserklaeringOgsendeMeldingTilMorDersomFarIkkeHarSignert(farskapserklaering);
      }
    }
  }

  private void sletteFarskapserklaeringOgsendeMeldingTilMorDersomFarIkkeHarSignert(Farskapserklaering farskapserklaering) {
    if (farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() == null
        || farskapserklaering.getDokument().getSigneringsinformasjonFar().getXadesXml() == null) {
      persistenceService.deaktivereFarskapserklaering(farskapserklaering.getId());
      log.info("Farskapserklæring med id {} ble slettet fra databasen.", farskapserklaering.getId());
      brukernotifikasjonConsumer.varsleMorOmUtgaattOppgaveForSignering(farskapserklaering.getMor().getFoedselsnummer());
      log.info("Varsel sendt til mor om at fars oppgave for signering er utgått, og at ny farskapserklæring må opprettes.");
    }
  }
}
