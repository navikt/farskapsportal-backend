package no.nav.farskapsportal.backend.lib.felles.consumer.brukernotifikasjon;

import java.time.LocalDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.builders.DoneBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.farskapsportal.backend.lib.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Ferdigprodusent {

  FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;
  KafkaTemplate kafkaTemplate;

  public void ferdigstilleFarsSigneringsoppgave(String idFarskapserklaering, String foedselsnummerFar) {

    var nokkel = new NokkelBuilder().withEventId(idFarskapserklaering).withSystembruker(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn())
        .build();
    var melding = oppretteDone(foedselsnummerFar);

    kafkaTemplate.send(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicFerdig(), nokkel, melding);
  }

  private Done oppretteDone(String foedselsnummerFar) {
    return new DoneBuilder()
        .withTidspunkt(LocalDateTime.now())
        .withFodselsnummer(foedselsnummerFar)
        .withGrupperingsId(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .build();
  }

}
