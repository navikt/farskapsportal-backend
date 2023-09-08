package no.nav.farskapsportal.backend.apps.api.scheduled.oppgave;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.Oppgave;
import no.nav.farskapsportal.backend.apps.api.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.apps.api.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.dto.oppgave.Oppgaveforespoersel;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Builder
public class Oppgavestyring {

  private Oppgave egenskaperOppgavestyring;
  private FarskapserklaeringDao farskapserklaeringDao;
  private OppgaveApiConsumer oppgaveApiConsumer;
  private PersonopplysningService personopplysningService;

  private static final String OPPGAVE_DATOFORMAT_I_BESKRIVELSE = "dd.MM.YYYY";

  private static final String OPPGAVEBESKRIVELSE_GENERELL =
      "ELEKTRONISK ERKLÆRING -"
          + " Farskap for %s er erklært elektronisk. Far (%s) har oppgitt at han ikke bor sammen med mor (%s). Vurder om det skal tas opp bidragssak.";

  @SchedulerLock(name = "oppgave", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
  @Scheduled(cron = "${farskapsportal.asynkron.egenskaper.oppgave.vurdere-opprettelse}")
  public void vurdereOpprettelseAvOppgave() {

    log.info("Vurderer opprettelse av oppgave for foreldre som ikke bor sammen ved fødsel.");
    var grenseverdiTermindato = LocalDate.now().minusWeeks(2);
    var grenseverdiSigneringstidspunktFar = LocalDateTime.now().with(LocalTime.MIDNIGHT);
    var farskapseerklaeringerDetSkalOpprettesOppgaverFor =
        farskapserklaeringDao.henteIdTilFarskapserklaeringerDetSkalOpprettesOppgaverFor(
            grenseverdiTermindato, grenseverdiSigneringstidspunktFar);

    log.info(
        "Fant {} farskapserklæringer som gjelder foreldre som ikke bor sammen som det skal opprettes oppgave om bidrag for.",
        farskapseerklaeringerDetSkalOpprettesOppgaverFor.size());

    if (egenskaperOppgavestyring.getMaksAntallOppgaverPerDag()
        < farskapseerklaeringerDetSkalOpprettesOppgaverFor.size()) {
      log.info(
          "Maks antall oppgaver per dag er lavere enn antall farskapserklæringer identifisert ({} vs {}). Forsøker å opprette"
              + " oppgave for {} erklæringer.",
          egenskaperOppgavestyring.getMaksAntallOppgaverPerDag(),
          farskapseerklaeringerDetSkalOpprettesOppgaverFor.size(),
          egenskaperOppgavestyring.getMaksAntallOppgaverPerDag());
    }

    var teller = 0;
    for (int id : farskapseerklaeringerDetSkalOpprettesOppgaverFor) {
      if (teller < egenskaperOppgavestyring.getMaksAntallOppgaverPerDag()) {
        var oppgaveid = oppretteOppgave(id);
        if (oppgaveid != -1) {
          teller++;
        }
      } else {
        log.info(
            "Dagligmaksimumsantall oppgaver er nådd, avslutter opprettelse av de resterende oppgavene.");
        break;
      }
    }

    log.info(
        "Det ble opprettet oppgave for {} av {} identifiserte farskapserklæringer",
        teller,
        farskapseerklaeringerDetSkalOpprettesOppgaverFor.size());
  }

  private long oppretteOppgave(int farskapserklaeringsId) {
    var farskapserklaering = farskapserklaeringDao.findById(farskapserklaeringsId);

    if (farskapserklaering.isPresent() && (farskapserklaering.get().getSendtTilSkatt() != null)) {
      var barn = farskapserklaering.get().getBarn();
      var far = farskapserklaering.get().getFar();
      var mor = farskapserklaering.get().getMor();

      var beskrivelse =
          barn.getFoedselsnummer() == null
              ? String.format(
                  OPPGAVEBESKRIVELSE_GENERELL,
                  "barn oppgitt med termin "
                      + barn.getTermindato()
                          .format(DateTimeFormatter.ofPattern(OPPGAVE_DATOFORMAT_I_BESKRIVELSE)),
                  far.getFoedselsnummer(),
                  mor.getFoedselsnummer())
              : String.format(
                  OPPGAVEBESKRIVELSE_GENERELL,
                  "barn med fødselsnummer " + barn.getFoedselsnummer(),
                  far.getFoedselsnummer(),
                  mor.getFoedselsnummer());

      var aktoerid =
          personopplysningService.henteAktoerid(
              farskapserklaering.get().getMor().getFoedselsnummer());

      if (aktoerid.isPresent()) {
        var oppgaveforespoersel =
            new Oppgaveforespoersel()
                .toBuilder().aktoerId(aktoerid.get()).beskrivelse(beskrivelse).build();
        var oppgaveId = oppgaveApiConsumer.oppretteOppgave(oppgaveforespoersel);

        if (oppgaveId != -1) {
          farskapserklaering.get().setOppgaveSendt(LocalDateTime.now());
          farskapserklaeringDao.save(farskapserklaering.get());
          log.info(
              "Oppgave opprettet for farskapserklæring med id {}",
              farskapserklaering.get().getId());
          return oppgaveId;
        } else {
          log.warn(
              "Opprettelse av oppgave feilet for farskapserklæring med id {}",
              farskapserklaering.get().getId());
        }
      } else {
        log.warn(
            "Fant ingen aktørid knyttet til mor i farskapserklæring med id {}. Oppgaven ble derfor ikke opprettet.",
            farskapserklaering.get().getId());
      }
    } else {
      log.error(
          "Feil i uttrekk for foreldre som ikke bor sammen. Farskapserklæring med id {} er enten ikke tilgjengelig, eller ikke sendt til Skatt!",
          farskapserklaering.get().getId());
    }
    return -1;
  }
}
