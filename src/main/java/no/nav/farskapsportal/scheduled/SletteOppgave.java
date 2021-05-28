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
public class SletteOppgave {

  private int synlighetOppgaveIDager;
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;

  // Kjøres hver morgen kl 06:00
  @Scheduled(cron = "0 0 06 00 * ?")
  public void sletteUtloepteSigneringsoppgaver() {
    var farskapserklaeringerSomVenterPaaFar = persistenceService.henteFarskapserklaeringerSomVenterPaaFarsSignatur();

    for (Farskapserklaering farskapserklaering : farskapserklaeringerSomVenterPaaFar) {
      if (farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt().toLocalDate()
          .isBefore(LocalDate.now().minusDays(synlighetOppgaveIDager))) {
        brukernotifikasjonConsumer
            .sletteFarsSigneringsoppgave(farskapserklaering.getId(), farskapserklaering.getFar().getFoedselsnummer());
      }
    }
  }
}
