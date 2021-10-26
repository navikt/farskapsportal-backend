package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import java.time.LocalDate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.entity.Oppgavebestilling;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Builder
@Slf4j
public class SletteOppgave {

  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Scheduled(initialDelayString = "${farskapsportal.asynkron.egenskaper.oppgaveslettingsforsinkelse}", fixedDelayString = "${farskapsportal.asynkron.egenskaper.oppgaveslettingsintervall}")
  public void sletteUtloepteSigneringsoppgaver() {
    var farskapserklaeringerSomVenterPaaFar = persistenceService.henteFarskapserklaeringerSomVenterPaaFarsSignatur();

    log.info("Ser etter utløpte signeringsoppgaver. Fant {} farskapserklæringer som venter på fars signatur.", farskapserklaeringerSomVenterPaaFar.size());

    for (Farskapserklaering farskapserklaering : farskapserklaeringerSomVenterPaaFar) {
      if (farskapserklaering.getDokument().getSigneringsinformasjonMor()
          .getSigneringstidspunkt().toLocalDate()
          .isBefore(LocalDate.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjonOppgaveSynlighetAntallDager() - 1))) {

        var aktiveOppgaver = persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(farskapserklaering.getId(),
            farskapserklaering.getFar());

        for (Oppgavebestilling oppgave : aktiveOppgaver) {

          log.info("Sletter utgått signeringsoppgave for far (id {}) i farskapserklæring (id {})", farskapserklaering.getFar().getId(),
              farskapserklaering.getId());

          brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(oppgave.getEventId(), farskapserklaering.getFar());
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
