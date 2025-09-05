package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.SIKKER_LOGG;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.InternFeilException;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.tms.varsel.action.Sensitivitet;
import no.nav.tms.varsel.action.Varseltype;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@AllArgsConstructor
public class Oppgaveprodusent {

  private KafkaTemplate kafkaTemplate;
  private PersistenceService persistenceService;
  private URL farskapsportalUrl;
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  public void oppretteOppgaveForSigneringAvFarskapserklaering(
      int idFarskapserklaering, Forelder far, String oppgavetekst, String varselId) {

    var melding = oppretteOppgave(oppgavetekst, farskapsportalUrl, far, varselId);

    var farsAktiveSigneringsoppgaver =
        persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(
            idFarskapserklaering, far);

    if (farsAktiveSigneringsoppgaver.isEmpty()) {
      log.info(
          "Oppretter oppgave om signering til far i farskapserklæring med id {}",
          idFarskapserklaering);
      oppretteOppgave(varselId, melding);
      log.info("Signeringsppgave opprettet for far");
      SIKKER_LOGG.info("Signeringsppgave opprettet for far med id {}.", far.getId());
      persistenceService.lagreNyOppgavebestilling(idFarskapserklaering, varselId);
    }
  }

  private void oppretteOppgave(String varselId, String melding) {
    try {
      kafkaTemplate.send(
          farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon(),
          varselId,
          melding);
    } catch (Exception e) {
      e.printStackTrace();
      throw new InternFeilException(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
    }
  }

  private String oppretteOppgave(
      String oppgavetekst, URL farskapsportalUrl, Forelder far, String varselId) {

    return OpprettVarselBuilder.newInstance()
        .withType(Varseltype.Oppgave)
        .withVarselId(varselId)
        .withSensitivitet(
            Sensitivitet.valueOf(
                farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave()))
        .withIdent(far.getFoedselsnummer())
        .withTekst("nb", oppgavetekst, true)
        .withLink(farskapsportalUrl.toString())
        .withAktivFremTil(
            ZonedDateTime.now(ZoneId.of("UTC"))
                .plusDays(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getLevetidOppgaveAntallDager()))
        .withEksternVarsling()
        .withProdusent(
            farskapsportalFellesEgenskaper.getCluster(),
            farskapsportalFellesEgenskaper.getNamespace(),
            farskapsportalFellesEgenskaper.getAppnavn())
        .build();
  }
}
