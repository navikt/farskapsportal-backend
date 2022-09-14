package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.libs.dto.oppgave.Oppgaveforespoersel;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Builder
@Validated
public class Oppgavestyring {

  private FarskapserklaeringDao farskapserklaeringDao;

  private OppgaveApiConsumer oppgaveApiConsumer;

  @Scheduled(cron = "${farskapsportal.asynkron.egenskaper.vurdere-opprettelse-av-oppgave}")
  public void vurdereOpprettelseAvOppgave() {

    log.info("Vurderer opprettelse av oppgave for foreldre som ikke bor sammen ved fødsel.");

    var grenseverdiTermindato = LocalDate.now().minusWeeks(2);
    var grenseverdiSigneringstidspunktFar = LocalDateTime.now().with(LocalTime.MIDNIGHT);
    var farskapseerklaeringerDetSkalOpprettesOppgaverFor = farskapserklaeringDao.henteIdTilFarskapserklaeringerDetSkalOpprettesOppgaverFor(grenseverdiTermindato, grenseverdiSigneringstidspunktFar);

    log.info("Fant {} farskapserklæringer som gjelder foreldre som ikke bor sammen som det skal opprettes oppgave om bidrag for.",
        farskapseerklaeringerDetSkalOpprettesOppgaverFor.size());

    for (int farskapserklaeringsId : farskapseerklaeringerDetSkalOpprettesOppgaverFor) {
      var farskapserklaering = farskapserklaeringDao.findById(farskapserklaeringsId);

      // Sletter oppgaver relatert til ferdigstilte eller deaktiverte erklæringer
      if (farskapserklaering.isPresent()
          && (farskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() != null
          || farskapserklaering.get().getDeaktivert() != null)) {

        var oppgaveforespoersel = new Oppgaveforespoersel().toBuilder().build();
            oppgaveApiConsumer.oppretteOppgave(oppgaveforespoersel);
      }
    }
  }

}
