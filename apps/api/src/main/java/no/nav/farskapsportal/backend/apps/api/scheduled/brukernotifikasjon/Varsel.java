package no.nav.farskapsportal.backend.apps.api.scheduled.brukernotifikasjon;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.Brukernotifikasjon;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Builder
public class Varsel {

  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;
  private Brukernotifikasjon egenskaperBrukernotifikasjon;

  @SchedulerLock(name = "manglende_signering", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
  @Scheduled(cron = "${farskapsportal.asynkron.egenskaper.brukernotifikasjon.varsle-om-uferdig-erklaering-cron}", zone = "Europe/Oslo")
  public void varsleOmManglendeSigneringsinfo() {

    var grensetidspunkt = LocalDateTime.now()
        .minusDays(egenskaperBrukernotifikasjon.getOppgavestyringsforsinkelse());

    var ider = persistenceService.henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoFar(grensetidspunkt);

    var farskapserklaering_tekst = ider.size() == 1 ? "farskapserklæring" : "farskapserklæringer";

    log.info("Fant id til {} {} som det sendes eksternt varsel til foreldrene om.", ider.size(), farskapserklaering_tekst);

    for (int id : ider) {
      var farskapserklaering = persistenceService.henteFarskapserklaeringForId(id);
      var sendtTilSignering = farskapserklaering.getDokument().getSigneringsinformasjonFar().getSendtTilSignering();
      var sendeVarsel = sendtTilSignering == null
          ? farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt().isBefore(grensetidspunkt)
          : sendtTilSignering.isBefore(grensetidspunkt);
      if (sendeVarsel) {
        brukernotifikasjonConsumer.varsleForeldreOmManglendeSignering(farskapserklaering.getMor(), farskapserklaering.getFar(),
            farskapserklaering.getBarn(), farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt().toLocalDate());
      } else {
        log.info("Varsel ikke sendt for farskapserklæring med id {} ettersom det var for kort tid siden denne ble oppdatert", id);
      }
    }
  }
}
