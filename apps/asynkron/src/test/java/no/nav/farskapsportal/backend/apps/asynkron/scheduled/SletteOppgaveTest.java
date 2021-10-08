package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.exception.Feilkode.FANT_IKKE_FARSKAPSERKLAERING;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SletteOppgave")
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalAsynkronTestApplication.class)
@ActiveProfiles(PROFILE_TEST)
public class SletteOppgaveTest {

  private static final int BRUKERNOTIFIKASJON_BESKJED_MND_SYNLIG = 1;
  private static final int BRUKERNOTIFIKASJON_SIKKERHETSNIVAA_BESKJED = 3;
  private static final String BRUKERNOTIFIKASJON_TOPIC_BESKJED = "aapen-brukernotifikasjon-nyBeskjed-v1";
  private static final String BRUKERNOTIFIKASJON_TOPIC_FERDIG = "aapen-brukernotifikasjon-done-v1";
  private static final String GRUPPERINGSID_FARSKAP = "farskap";
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Farskapserklæringen er derfor slettet. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private static final String URL_FARSKAPSPORTAL = "https://farskapsportal.dev.nav.no/nb/";

  @Autowired
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @MockBean
  private KafkaTemplate<Nokkel, Beskjed> beskjedkoe;

  @MockBean
  private KafkaTemplate<Nokkel, Done> ferdigkoe;

  private SletteOppgave sletteOppgave;

  @BeforeEach
  void setup() {

    // Bønnen sletteOppgave er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    sletteOppgave = SletteOppgave.builder()
        .persistenceService(persistenceService)
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .build();
  }

  @Test
  void skalSletteUtloeptOppgaveOgVarsleMorDersomFarIkkeSignererInnenFristen() {

    // given
    var tidspunktFoerTestIEpochMillis = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(5).toInstant().toEpochMilli();
    farskapserklaeringDao.deleteAll();
    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    // Setter signeringstidspunkt til utenfor levetiden til oppgaven
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjonOppgaveSynlighetAntallDager()));

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);
    var ferdignoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    verify(ferdigkoe, times(1))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_FERDIG), ferdignoekkelfanger.capture(), ferdigfanger.capture());
    verify(beskjedkoe, times(1))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED), beskjednoekkelfanger.capture(),
            beskjedfanger.capture());

    var ferdignokkel = ferdignoekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    var beskjednoekkel = beskjednoekkelfanger.getAllValues().get(0);
    var beskjed = beskjedfanger.getAllValues().get(0);

    var beskjedMorSynligFremTilDato = Instant.ofEpochMilli(beskjed.getSynligFremTil()).atZone(ZoneId.systemDefault()).toLocalDate();

    assertAll(
        () -> assertThat(ferdignokkel.getSystembruker()).isEqualTo(farskapsportalAsynkronEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(ferdig.getGrupperingsId()).isEqualTo(GRUPPERINGSID_FARSKAP),
        () -> assertThat(ferdig.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestIEpochMillis),
        () -> assertThat(beskjednoekkel.getSystembruker()).isEqualTo(farskapsportalAsynkronEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(GRUPPERINGSID_FARSKAP),
        () -> assertThat(beskjed.getLink()).isEqualTo(URL_FARSKAPSPORTAL),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(BRUKERNOTIFIKASJON_SIKKERHETSNIVAA_BESKJED),
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(beskjed.getTekst()).isEqualTo(MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE),
        () -> assertThat(beskjed.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedMorSynligFremTilDato).isEqualTo(LocalDate.now().plusMonths(BRUKERNOTIFIKASJON_BESKJED_MND_SYNLIG))
    );

    var ressursIkkeFunnetException = assertThrows(RessursIkkeFunnetException.class,
        () -> persistenceService.henteFarskapserklaeringForId(farskapserklaering.getId()));

    // Den lagrede farskapserklæringen skal deaktiveres i forbindelse med at melding sendes til mor om utgått signeringsoppgave
    assertThat(ressursIkkeFunnetException.getFeilkode()).isEqualTo(FANT_IKKE_FARSKAPSERKLAERING);

    var deaktivertFarskapserklaering = farskapserklaeringDao.findById(farskapserklaering.getId());

    assertAll(
        () -> assertThat(deaktivertFarskapserklaering).isPresent(),
        () -> assertThat(deaktivertFarskapserklaering.get().getDeaktivert()).isNotNull()
    );
  }

  @Test
  void skalIkkeSletteOppgaveSomIkkeErUtloept() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    // Setter signeringstidspunkt til innenfor levetiden til oppgaven
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjonOppgaveSynlighetAntallDager() - 5));

    persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    verify(ferdigkoe, times(0))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_FERDIG), any(), any());
  }
}
