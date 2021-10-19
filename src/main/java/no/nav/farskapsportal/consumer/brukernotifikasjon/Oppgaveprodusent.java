package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.exception.InternFeilException;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.service.PersistenceService;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@AllArgsConstructor
public class Oppgaveprodusent {

  private FarskapsportalEgenskaper farskapsportalEgenskaper;
  private KafkaTemplate kafkaTemplate;
  private PersistenceService persistenceService;

  public void oppretteOppgaveForSigneringAvFarskapserklaering(int idFarskapserklaering, Forelder far, String oppgavetekst,
      boolean medEksternVarsling, URL farskapsportalUrl) {

    var nokkel = new NokkelBuilder().withEventId(UUID.randomUUID().toString()).withSystembruker(farskapsportalEgenskaper.getSystembrukerBrukernavn())
        .build();
    var melding = oppretteOppgave(far.getFoedselsnummer(), oppgavetekst, medEksternVarsling, farskapsportalUrl);

    try {
      kafkaTemplate.send(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicOppgave(), nokkel, melding);
    } catch (Exception e) {
      log.error("Opprettelse av oppgave feilet!");
      e.printStackTrace();
      throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
    }

    log.info("Signeringsppgave opprettet for far med id {}.", far.getId());
    persistenceService.lagreNyOppgavebestilling(idFarskapserklaering, nokkel.getEventId());
  }

  private Oppgave oppretteOppgave(String foedselsnummer, String oppgavetekst, boolean medEksternVarsling, URL farskapsportalUrl) {

    return new OppgaveBuilder()
        .withTidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withLink(farskapsportalUrl)
        .withSikkerhetsnivaa(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave())
        .withTekst(oppgavetekst).build();
  }
}
