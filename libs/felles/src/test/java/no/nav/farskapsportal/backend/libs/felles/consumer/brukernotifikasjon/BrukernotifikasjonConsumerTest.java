package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.DOKUMENT;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.BrukernotifikasjonConfig;
import no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Teste BrukernotifikasjonConsumer")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {FarskapsportalFellesTestConfig.class,
    BrukernotifikasjonConfig.class})
@ActiveProfiles(PROFILE_TEST)
public class BrukernotifikasjonConsumerTest {

  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING = "Du har mottatt en farskapserklæring som venter på din signatur.";
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Farskapserklæringen er derfor slettet. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private static final String MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private static final String MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig.";

  private static final Barn BARN = TestUtils.henteBarnUtenFnr(5);

  @Autowired
  BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  PersistenceService persistenceService;

  @Autowired
  FarskapserklaeringDao farskapserklaeringDao;

  @MockBean
  private KafkaTemplate<Nokkel, Beskjed> beskjedkoe;

  @MockBean
  private KafkaTemplate<Nokkel, Done> ferdigkoe;

  @MockBean
  private KafkaTemplate<Nokkel, Oppgave> oppgavekoe;

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Test
  void skalInformereForeldreOmTilgjengeligFarskapserklaering() {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    brukernotifikasjonConsumer.informereForeldreOmTilgjengeligFarskapserklaering(MOR.getFoedselsnummer(), FAR.getFoedselsnummer());

    // then
    verify(beskjedkoe, times(2))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

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
        () -> assertThat(noekkelMor.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(noekkelFar.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjedMor.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedFar.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedMor.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedFar.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedMor.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(beskjedFar.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(beskjedMor.getSikkerhetsnivaa()).isEqualTo(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjedFar.getSikkerhetsnivaa()).isEqualTo(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjedMor.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedFar.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedMor.getLink()).isEqualTo(farskapsportalFellesEgenskaper.getUrl()),
        () -> assertThat(beskjedFar.getLink()).isEqualTo(farskapsportalFellesEgenskaper.getUrl()),
        () -> assertThat(beskjedMorSynligFremTilDato)
            .isEqualTo(LocalDate.now().plusMonths(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder())),
        () -> assertThat(beskjedFarSynligFremTilDato)
            .isEqualTo(LocalDate.now().plusMonths(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
    );
  }

  @Test
  void skalVarsleMorOmUtloeptOppgaveForSignering() {

    // given
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    brukernotifikasjonConsumer.varsleMorOmUtgaattOppgaveForSignering(MOR.getFoedselsnummer());

    // then
    verify(beskjedkoe, times(1))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var nokkel = noekkelfanger.getValue();
    var beskjed = beskjedfanger.getValue();

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjed.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
<<<<<<< HEAD:libs/felles/src/test/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/BrukernotifikasjonConsumerTest.java
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
=======
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
>>>>>>> main:src/test/java/no/nav/farskapsportal/consumer/brukernotifikasjon/BrukernotifikasjonConsumerTest.java
        () -> assertThat(beskjed.getTidspunkt()).isBetween(Instant.now().minusSeconds(5).toEpochMilli(), Instant.now().toEpochMilli()),
        () -> assertThat(beskjed.getTekst()).isEqualTo(MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalFellesEgenskaper.getUrl())
    );
  }

  @Test
  void skalVarsleMorOgFarDersomFarAvbryterSignering() {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    brukernotifikasjonConsumer.varsleOmAvbruttSignering(MOR.getFoedselsnummer(), FAR.getFoedselsnummer());

    // then
    verify(beskjedkoe, times(2))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var alleBeskjeder = beskjedfanger.getAllValues();
    var beskjedTilMor = alleBeskjeder.get(0);
    var beskjedTilFar = alleBeskjeder.get(1);

    var alleNoekler = noekkelfanger.getAllValues();
    var noekkelTilMor = alleNoekler.get(0);
    var noekkelTilFar = alleNoekler.get(1);

    assertAll(
        () -> assertThat(noekkelTilMor.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjedTilMor.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedTilMor.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
<<<<<<< HEAD:libs/felles/src/test/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/BrukernotifikasjonConsumerTest.java
        () -> assertThat(beskjedTilMor.getGrupperingsId()).isEqualTo(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedTilMor.getSikkerhetsnivaa()).isEqualTo(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
=======
        () -> assertThat(beskjedTilMor.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedTilMor.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
>>>>>>> main:src/test/java/no/nav/farskapsportal/consumer/brukernotifikasjon/BrukernotifikasjonConsumerTest.java
        () -> assertThat(beskjedTilMor.getTidspunkt()).isBetween(Instant.now().minusSeconds(5).toEpochMilli(), Instant.now().toEpochMilli()),
        () -> assertThat(beskjedTilMor.getTekst()).isEqualTo(MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING),
        () -> assertThat(beskjedTilMor.getLink()).isEqualTo(farskapsportalFellesEgenskaper.getUrl())
    );

    assertAll(
        () -> assertThat(noekkelTilFar.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjedTilFar.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedTilFar.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
<<<<<<< HEAD:libs/felles/src/test/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/BrukernotifikasjonConsumerTest.java
        () -> assertThat(beskjedTilFar.getGrupperingsId()).isEqualTo(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedTilFar.getSikkerhetsnivaa()).isEqualTo(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
=======
        () -> assertThat(beskjedTilFar.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjedTilFar.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
>>>>>>> main:src/test/java/no/nav/farskapsportal/consumer/brukernotifikasjon/BrukernotifikasjonConsumerTest.java
        () -> assertThat(beskjedTilFar.getTidspunkt()).isBetween(Instant.now().minusSeconds(5).toEpochMilli(), Instant.now().toEpochMilli()),
        () -> assertThat(beskjedTilFar.getTekst()).isEqualTo(MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING),
        () -> assertThat(beskjedTilFar.getLink()).isEqualTo(farskapsportalFellesEgenskaper.getUrl())
    );
  }

  @Test
  void skalOppretteOppgaveTilFarOmSignering() {

    // given
    farskapserklaeringDao.deleteAll();
    var farskapserklaeringSomVenterPaaFarsSignatur = Farskapserklaering.builder().mor(MOR).far(FAR).barn(BARN).dokument(DOKUMENT).build();
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var oppgavefanger = ArgumentCaptor.forClass(Oppgave.class);

    // when
    brukernotifikasjonConsumer.oppretteOppgaveTilFarOmSignering(farskapserklaering.getId(), FAR.getFoedselsnummer());

    // then
    verify(oppgavekoe, times(1))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicOppgave()), noekkelfanger.capture(), oppgavefanger.capture());

    var nokkel = noekkelfanger.getValue();
    var oppgave = oppgavefanger.getValue();

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(nokkel.getEventId()).isEqualTo(Integer.toString(farskapserklaering.getId())),
        () -> assertThat(oppgave.getEksternVarsling()).isTrue(),
        () -> assertThat(oppgave.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
<<<<<<< HEAD:libs/felles/src/test/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/BrukernotifikasjonConsumerTest.java
        () -> assertThat(oppgave.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(oppgave.getSikkerhetsnivaa()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave()),
=======
        () -> assertThat(oppgave.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(oppgave.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave()),
>>>>>>> main:src/test/java/no/nav/farskapsportal/consumer/brukernotifikasjon/BrukernotifikasjonConsumerTest.java
        () -> assertThat(oppgave.getTidspunkt()).isBetween(Instant.now().minusSeconds(5).toEpochMilli(), Instant.now().toEpochMilli()),
        () -> assertThat(oppgave.getTekst()).isEqualTo(MELDING_OM_VENTENDE_FARSKAPSERKLAERING),
        () -> assertThat(oppgave.getLink()).isEqualTo(farskapsportalFellesEgenskaper.getUrl())
    );
  }

  @Test
  void skalSletteFarsSigneringsoppgave() {

    // given
    farskapserklaeringDao.deleteAll();

    var dokument = Dokument.builder().navn("farskapserklaering.pdf")
        .signeringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(lageUrl("redirect-mor")).build())
        .signeringsinformasjonFar(Signeringsinformasjon.builder().redirectUrl(lageUrl("/redirect-far")).build())
        .build();
    var farskapserklaeringSomVenterPaaFarsSignatur = Farskapserklaering.builder().mor(henteForelder(Forelderrolle.MOR))
        .far(henteForelder(Forelderrolle.FAR)).barn(TestUtils.henteBarnUtenFnr(5)).dokument(dokument).build();
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);

    // when
    brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(farskapserklaering.getId(), FAR.getFoedselsnummer());

    // then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicFerdig()), noekkelfanger.capture(), ferdigfanger.capture());

    var nokkel = noekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(ferdig.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(ferdig.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThan(Instant.now().minusSeconds(10).toEpochMilli())
    );
  }
}
