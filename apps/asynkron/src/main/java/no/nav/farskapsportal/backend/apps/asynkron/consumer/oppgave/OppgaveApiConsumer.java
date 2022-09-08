package no.nav.farskapsportal.backend.apps.asynkron.consumer.oppgave;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.dto.oppgave.OppretteOppgaveForespoersel;
import no.nav.farskapsportal.backend.libs.dto.oppgave.OppretteOppgaveRespons;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Slf4j
@AllArgsConstructor
public class OppgaveApiConsumer {

  private final RestTemplate restTemplate;

  public long oppretteOppgave(OppretteOppgaveForespoersel opprettOppgaveforespoersel) {

    SIKKER_LOGG.debug("oppretter oppgave: " + opprettOppgaveforespoersel);
    log.info("oppretter oppgave med type {}", opprettOppgaveforespoersel.getOppgavetype());

    var oppgaveResponse = restTemplate.postForEntity("/", opprettOppgaveforespoersel, OppretteOppgaveRespons.class);

    SIKKER_LOGG.debug("oppgaveResponse: " + oppgaveResponse);

    return Optional.of(oppgaveResponse)
        .map(ResponseEntity::getBody)
        .map(OppretteOppgaveRespons::getId)
        .orElse(-1L);
  }
}
