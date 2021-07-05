package no.nav.farskapsportal.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Teste BrukernotifikasjonConsumer")
@SpringBootTest(classes = FarskapsportalApplicationLocal.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(PROFILE_TEST)
public class BrukernotifikasjonConsumerTest {

  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING = "Du har mottatt en farskapserklæring som venter på din signatur.";
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Trykk her for å opprette ny farskapserklæring.";
  private static final String MELDING_OM_AVBRUTT_SIGNERING = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Trykk her for å opprette ny farskapserklæring.";

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto BARN = henteBarnUtenFnr(5);

  @Autowired
  BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  PersistenceService persistenceService;

  @Autowired
  FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  Mapper mapper;

  @MockBean
  private KafkaTemplate<Nokkel, Beskjed> beskjedkoe;

  @MockBean
  private KafkaTemplate<Nokkel, Done> ferdigkoe;

  @MockBean
  private KafkaTemplate<Nokkel, Oppgave> oppgavekoe;

  @Autowired
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  @Test
  void skalInformereForeldreOmTilgjengeligFarskapserklaering() {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    brukernotifikasjonConsumer.informereForeldreOmTilgjengeligFarskapserklaering(MOR.getFoedselsnummer(), FAR.getFoedselsnummer());

    // then
    verify(beskjedkoe, times(2))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var beskjeder = beskjedfanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(2),
        () -> assertThat(beskjeder.size()).isEqualTo(2));

    var noekkelMor = noekler.get(0);
    var noekkelFar = noekler.get(1);
    var beskjedMor = beskjeder.get(0);
    var beskjedFar = beskjeder.get(1);

    var beskjedMorSynligFremTilDato = Instant.ofEpochMilli(beskjedMor.getSynligFremTil()).atZone(ZoneId.systemDefault()).toLocalDate();
    var beskjedFarSynligFremTilDato = Instant.ofEpochMilli(beskjedFar.getSynligFremTil()).atZone(ZoneId.systemDefault()).toLocalDate();

    assertAll(
        () -> assertThat(noekkelMor.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(noekkelFar.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjedMor.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedFar.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedMor.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedFar.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedMor.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(beskjedFar.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(beskjedMor.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjedFar.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjedMor.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedFar.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedMor.getLink()).isEqualTo(farskapsportalEgenskaper.getUrl()),
        () -> assertThat(beskjedFar.getLink()).isEqualTo(farskapsportalEgenskaper.getUrl()),
        () -> assertThat(beskjedMorSynligFremTilDato)
            .isEqualTo(LocalDate.now().plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder())),
        () -> assertThat(beskjedFarSynligFremTilDato)
            .isEqualTo(LocalDate.now().plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
    );
  }

  @Test
  void skalVarsleMorOmUtloeptOppgaveForSignering() {

    // given
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    var tidspunktFoerTestEpochMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    // when
    brukernotifikasjonConsumer.varsleMorOmUtgaattOppgaveForSignering(MOR.getFoedselsnummer());

    // then
    verify(beskjedkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var nokkel = noekkelfanger.getValue();
    var beskjed = beskjedfanger.getValue();
    var tidspunktEtterTestEpocMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjed.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjed.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestEpochMillis),
        () -> assertThat(beskjed.getTidspunkt()).isLessThanOrEqualTo(tidspunktEtterTestEpocMillis),
        () -> assertThat(beskjed.getTekst()).isEqualTo(MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalEgenskaper.getUrl())
    );
  }

  @Test
  void skalVarsleMorDersomFarAvbryterSignering() {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    var tidspunktFoerTestEpochMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    // when
    brukernotifikasjonConsumer.varsleMorOmAvbruttSignering(MOR.getFoedselsnummer());

    // then
    verify(beskjedkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var nokkel = noekkelfanger.getValue();
    var beskjed = beskjedfanger.getValue();
    var tidspunktEtterTestEpocMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjed.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjed.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestEpochMillis),
        () -> assertThat(beskjed.getTidspunkt()).isLessThanOrEqualTo(tidspunktEtterTestEpocMillis),
        () -> assertThat(beskjed.getTekst()).isEqualTo(MELDING_OM_AVBRUTT_SIGNERING),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalEgenskaper.getUrl())
    );
  }

  @Test
  void skalOppretteOppgaveTilFarOmSignering() {

    // given
    farskapserklaeringDao.deleteAll();
    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(mapper.toEntity(farskapserklaeringSomVenterPaaFarsSignatur));

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var oppgavefanger = ArgumentCaptor.forClass(Oppgave.class);

    var tidspunktFoerTestEpochMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    // when
    brukernotifikasjonConsumer.oppretteOppgaveTilFarOmSignering(farskapserklaering.getId(), FAR.getFoedselsnummer());

    // then
    verify(oppgavekoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicOppgave()), noekkelfanger.capture(), oppgavefanger.capture());

    var nokkel = noekkelfanger.getValue();
    var oppgave = oppgavefanger.getValue();
    var tidspunktEtterTestEpocMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(nokkel.getEventId()).isEqualTo(Integer.toString(farskapserklaering.getId())),
        () -> assertThat(oppgave.getEksternVarsling()).isTrue(),
        () -> assertThat(oppgave.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(oppgave.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(oppgave.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave()),
        () -> assertThat(oppgave.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestEpochMillis),
        () -> assertThat(oppgave.getTidspunkt()).isLessThanOrEqualTo(tidspunktEtterTestEpocMillis),
        () -> assertThat(oppgave.getTekst()).isEqualTo(MELDING_OM_VENTENDE_FARSKAPSERKLAERING),
        () -> assertThat(oppgave.getLink()).isEqualTo(farskapsportalEgenskaper.getUrl())
    );
  }

  @Test
  void skalSletteFarsSigneringsoppgave() {

    // given
    farskapserklaeringDao.deleteAll();
    var tidspunktFoerTestIEpochMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(mapper.toEntity(farskapserklaeringSomVenterPaaFarsSignatur));

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);

    // when
    brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(farskapserklaering.getId(), FAR.getFoedselsnummer());

    // then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig()), noekkelfanger.capture(), ferdigfanger.capture());

    var nokkel = noekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(ferdig.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(ferdig.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestIEpochMillis)
    );
  }

}
