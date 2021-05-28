package no.nav.farskapsportal.consumer.brukernotifikasjon;

import java.time.LocalDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.builders.DoneBuilder;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Ferdigprodusent {

  FarskapsportalEgenskaper farskapsportalEgenskaper;
  KafkaTemplate kafkaTemplate;

  public void ferdigstilleFarsSigneringsoppgave(String idFarskapserklaering, String foedselsnummerFar) {

    var nokkel = new NokkelBuilder().withEventId(idFarskapserklaering).withSystembruker(farskapsportalEgenskaper.getSystembrukerBrukernavn()).build();
    var melding = oppretteDone(foedselsnummerFar);

    kafkaTemplate.send(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig(), nokkel, melding);
  }

  private Done oppretteDone(String foedselsnummerFar) {
    return new DoneBuilder()
        .withTidspunkt(LocalDateTime.now())
        .withFodselsnummer(foedselsnummerFar)
        .withGrupperingsId(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .build();
  }

}
