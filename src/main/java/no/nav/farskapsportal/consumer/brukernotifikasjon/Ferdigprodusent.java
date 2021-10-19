package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.DoneBuilder;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.exception.InternFeilException;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.service.PersistenceService;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Ferdigprodusent {

  FarskapsportalEgenskaper farskapsportalEgenskaper;
  KafkaTemplate kafkaTemplate;
  PersistenceService persistenceService;

  public void ferdigstilleFarsSigneringsoppgave(String foedselsnummerFar, Nokkel nokkel) {
    var melding = oppretteDone(foedselsnummerFar);

    try {
      kafkaTemplate.send(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig(), nokkel, melding);
    } catch (Exception e) {
      throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
    }
    log.info("Ferdigmelding ble sendt for oppgave med eventId {}.");
    persistenceService.setteOppgaveTilFerdigstilt(nokkel.getEventId());
  }

  private Done oppretteDone(String foedselsnummerFar) {
    return new DoneBuilder()
        .withTidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
        .withFodselsnummer(foedselsnummerFar)
        .withGrupperingsId(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .build();
  }
}
