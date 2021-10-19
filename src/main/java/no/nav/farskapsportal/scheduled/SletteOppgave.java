package no.nav.farskapsportal.scheduled;

import java.time.LocalDate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Oppgavebestilling;
import no.nav.farskapsportal.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Builder
@Slf4j
public class SletteOppgave {

  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  // Kjøres hver morgen kl 04:00
  @Scheduled(cron = "0 00 4 * * ?")
  public void sletteUtloepteSigneringsoppgaver() {
    var farskapserklaeringerSomVenterPaaFar = persistenceService.henteFarskapserklaeringerSomVenterPaaFarsSignatur();

    for (Farskapserklaering farskapserklaering : farskapserklaeringerSomVenterPaaFar) {
      if (farskapsportalEgenskaper.getBrukernotifikasjon().isSkruddPaa() && farskapserklaering.getDokument().getSigneringsinformasjonMor()
          .getSigneringstidspunkt().toLocalDate()
          .isBefore(LocalDate.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager() - 1))) {

        var aktiveOppgaver = persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(farskapserklaering.getId(),
            farskapserklaering.getFar());

        for (Oppgavebestilling oppgave : aktiveOppgaver) {

          log.info("Sletter utgått signeringsoppgave for far (id {}) i farskapserklæring (id {})", farskapserklaering.getFar().getId(),
              farskapserklaering.getId());

          brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(oppgave.getEventId(), farskapserklaering.getFar().getFoedselsnummer());
          sletteFarskapserklaeringOgsendeMeldingTilMorDersomFarIkkeHarSignert(farskapserklaering);
        }
      }
    }
  }

  private void sletteFarskapserklaeringOgsendeMeldingTilMorDersomFarIkkeHarSignert(Farskapserklaering farskapserklaering) {
    if (farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() == null
        || farskapserklaering.getDokument().getSigneringsinformasjonFar().getXadesXml() == null) {
      persistenceService.deaktivereFarskapserklaering(farskapserklaering.getId());
      log.info("Farskapserklæring med id {} ble slettet fra databasen.", farskapserklaering.getId());
      brukernotifikasjonConsumer.varsleMorOmUtgaattOppgaveForSignering(farskapserklaering.getMor());
      log.info("Varsel sendt til mor om at fars oppgave for signering er utgått, og at ny farskapserklæring må opprettes.");
    }
  }
}
