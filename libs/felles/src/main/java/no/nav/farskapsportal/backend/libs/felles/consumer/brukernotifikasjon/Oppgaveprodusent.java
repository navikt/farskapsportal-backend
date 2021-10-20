package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveBuilder;
<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Oppgaveprodusent.java
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
=======
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.exception.InternFeilException;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.service.PersistenceService;
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Oppgaveprodusent.java
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@AllArgsConstructor
public class Oppgaveprodusent {

<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Oppgaveprodusent.java
  private URL farskapsportalUrl;
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;
=======
  private FarskapsportalEgenskaper farskapsportalEgenskaper;
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Oppgaveprodusent.java
  private KafkaTemplate kafkaTemplate;
  private PersistenceService persistenceService;

  public void oppretteOppgaveForSigneringAvFarskapserklaering(int idFarskapserklaering, Forelder far, String oppgavetekst,
      boolean medEksternVarsling, URL farskapsportalUrl) {

<<<<<<< HEAD:libs/felles/src/main/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/Oppgaveprodusent.java
    var nokkel = new NokkelBuilder().withEventId(idFarskapserklaering).withSystembruker(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn())
        .build();
    var melding = oppretteOppgave(foedselsnummerFar, oppgavetekst, medEksternVarsling);
=======
    var nokkel = new NokkelBuilder().withEventId(UUID.randomUUID().toString()).withSystembruker(farskapsportalEgenskaper.getSystembrukerBrukernavn())
        .build();
    var melding = oppretteOppgave(far.getFoedselsnummer(), oppgavetekst, medEksternVarsling, farskapsportalUrl);

    var farsAktiveSigneringsoppgaver = persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(idFarskapserklaering, far);

    if (farsAktiveSigneringsoppgaver.isEmpty()) {
      log.info("Oppretter oppgave om signering til far i farskapserklæring med id {}", idFarskapserklaering);
      oppretteOppgave(nokkel, melding);
      log.info("Signeringsppgave opprettet for far med id {}.", far.getId());
      persistenceService.lagreNyOppgavebestilling(idFarskapserklaering, nokkel.getEventId());
    }
  }

  private void oppretteOppgave(Nokkel nokkel, Oppgave melding) {
>>>>>>> main:src/main/java/no/nav/farskapsportal/consumer/brukernotifikasjon/Oppgaveprodusent.java
    try {
      kafkaTemplate.send(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicOppgave(), nokkel, melding);
    } catch (Exception e) {
      e.printStackTrace();
      throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
    }
  }

  private Oppgave oppretteOppgave(String foedselsnummer, String oppgavetekst, boolean medEksternVarsling, URL farskapsportalUrl) {

    return new OppgaveBuilder()
        .withTidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
        .withEksternVarsling(medEksternVarsling)
        .withLink(farskapsportalUrl)
        .withSikkerhetsnivaa(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave())
        .withTekst(oppgavetekst).build();
  }
}
