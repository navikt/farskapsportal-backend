package no.nav.farskapsportal.scheduled;

import java.time.LocalDate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Builder
@Slf4j
public class VarsleFarOmSigneringsoppgave {

  private int antallDagerSidenMorSignerte;
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;

  // Kjøres hver morgen kl 06:00
  @Scheduled(cron = "0 0 06 00 * ?")
  public void varsleFedreOmVentendeSigneringsoppgaver() {
    var farskapserklaeringerSomVenterPaaFar = persistenceService.henteFarskapserklaeringerSomVenterPaaFarsSignatur();

    for (Farskapserklaering farskapserklaering : farskapserklaeringerSomVenterPaaFar) {
      if (farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt().toLocalDate()
          .isBefore(LocalDate.now().minusDays(antallDagerSidenMorSignerte))) {
        // Sende eksternt varsel til far om ventende signeringsoppgave
        brukernotifikasjonConsumer.varsleFarOmSigneringsoppgave(farskapserklaering.getFar().getFoedselsnummer());
      }
    }
  }
}
