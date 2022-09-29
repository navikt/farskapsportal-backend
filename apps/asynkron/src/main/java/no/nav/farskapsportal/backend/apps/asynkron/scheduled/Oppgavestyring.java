package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.api.FarskapsportalApiConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.libs.dto.asynkroncontroller.HenteAktoeridRequest;
import no.nav.farskapsportal.backend.libs.dto.oppgave.Oppgaveforespoersel;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Builder
@Validated
public class Oppgavestyring {

  private FarskapsportalApiConsumer farskapsportalApiConsumer;
  private FarskapserklaeringDao farskapserklaeringDao;
  private OppgaveApiConsumer oppgaveApiConsumer;

  private static final String OPPGAVE_DATOFORMAT_I_BESKRIVELSE = "dd.MM.YYYY";

  private static final String OPPGAVEBESKRIVELSE_GENERELL = "ELEKTRONISK ERKLÆRING -"
      + " Farskap for {} er erklært elektronisk. Far har oppgitt at han ikke bor sammen med mor. Vurder om det skal tas opp bidragssak.";

  @Scheduled(cron = "${farskapsportal.asynkron.egenskaper.vurdere-opprettelse-av-oppgave}")
  public int vurdereOpprettelseAvOppgave() {

    log.info("Vurderer opprettelse av oppgave for foreldre som ikke bor sammen ved fødsel.");
    var grenseverdiTermindato = LocalDate.now().minusWeeks(2);
    var grenseverdiSigneringstidspunktFar = LocalDateTime.now().with(LocalTime.MIDNIGHT);
    var farskapseerklaeringerDetSkalOpprettesOppgaverFor = farskapserklaeringDao.henteIdTilFarskapserklaeringerDetSkalOpprettesOppgaverFor(
        grenseverdiTermindato, grenseverdiSigneringstidspunktFar);

    log.info("Fant {} farskapserklæringer som gjelder foreldre som ikke bor sammen som det skal opprettes oppgave om bidrag for.",
        farskapseerklaeringerDetSkalOpprettesOppgaverFor.size());

    var teller = 0;
    for (int farskapserklaeringsId : farskapseerklaeringerDetSkalOpprettesOppgaverFor) {
      var farskapserklaering = farskapserklaeringDao.findById(farskapserklaeringsId);

      if (farskapserklaering.isPresent() && (farskapserklaering.get().getSendtTilSkatt() != null)) {
        var barn = farskapserklaering.get().getBarn();
        var beskrivelse = barn.getFoedselsnummer() == null
            ? String.format(OPPGAVEBESKRIVELSE_GENERELL,
            "barn oppgitt med termin " + barn.getTermindato().format(DateTimeFormatter.ofPattern(OPPGAVE_DATOFORMAT_I_BESKRIVELSE)))
            : String.format(OPPGAVEBESKRIVELSE_GENERELL, "barnet");

        var aktoerid = farskapsportalApiConsumer.henteAktoerid(
            HenteAktoeridRequest.builder().personident(farskapserklaering.get().getMor().getFoedselsnummer()).build());

        if (aktoerid.isPresent()) {
          var oppgaveforespoersel = new Oppgaveforespoersel().toBuilder()
              .aktoerId(aktoerid.get())
              .beskrivelse(beskrivelse).build();
          oppgaveApiConsumer.oppretteOppgave(oppgaveforespoersel);
          farskapserklaering.get().setOppgaveSendt(LocalDateTime.now());
          farskapserklaeringDao.save(farskapserklaering.get());
          teller++;
          log.info("Oppgave sendt for farskapserklæring med id {}", farskapserklaering.get().getId());
        } else {
          log.warn("Fant ingen aktørid knyttet til mor i farskapserklæring med id {}. Oppgaven ble derfor ikke opprettet.",
              farskapserklaering.get().getId());
        }
      } else {
        log.error(
            "Feil i uttrekk for foreldre som ikke bor sammen. Farskapserklæring med id {} er enten ikke tilgjengelig, eller ikke sendt til Skatt!",
            farskapserklaering.get().getId());
      }
    }

    var antallErklaeringerDetIkkeBleOpprettetOppgaveFor = farskapseerklaeringerDetSkalOpprettesOppgaverFor.size() - teller;

    if (antallErklaeringerDetIkkeBleOpprettetOppgaveFor < 1) {
      log.info("Det ble opprettet oppgave for {} av {} identifiserte farskapserklæringer", teller,
          farskapseerklaeringerDetSkalOpprettesOppgaverFor.size());
    } else {
      log.warn("Oppgave ble ikke opprettet for {} av de {} identifiserte farskapserklæringene", antallErklaeringerDetIkkeBleOpprettetOppgaveFor,
          antallErklaeringerDetIkkeBleOpprettetOppgaveFor);
    }
    return teller;
  }
}
