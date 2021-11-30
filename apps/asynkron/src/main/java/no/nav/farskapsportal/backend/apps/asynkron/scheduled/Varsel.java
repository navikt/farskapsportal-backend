package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Builder
public class Varsel {

  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private PersistenceService persistenceService;
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Scheduled(cron = "${farskapsportal.asynkron.egenskaper.varsle-om-uferdig-erklaering-cron}", zone = "Europe/Oslo")
  public void varsleOmManglendeSigneringsinfo() {

    var signertAvMorFoer = LocalDateTime.now()
        .minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse());

    var ider = persistenceService.henteIdTilAktiveFarskapserklaeringerSomManglerSigneringsinfoFar(signertAvMorFoer);

    var farskapserklaering_tekst = ider.size() == 1 ? "farskapserklæring" : "farskapserklæringer";

    log.info("Fant id til {} {} som det sendes eksternt varsel til foreldrene om.", ider.size(), farskapserklaering_tekst);

    for (int id : ider) {
      var farskapserklaering = persistenceService.henteFarskapserklaeringForId(id);
      brukernotifikasjonConsumer.varsleForeldreOmManglendeSignering(farskapserklaering.getMor(), farskapserklaering.getFar(),
          farskapserklaering.getBarn(), farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt().toLocalDate());
    }
  }
}
