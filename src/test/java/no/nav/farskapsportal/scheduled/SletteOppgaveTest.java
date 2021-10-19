package no.nav.farskapsportal.scheduled;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static no.nav.farskapsportal.api.Feilkode.FANT_IKKE_FARSKAPSERKLAERING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.persistence.entity.Oppgavebestilling;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SletteOppgave")
@SpringBootTest(classes = FarskapsportalApplicationLocal.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(PROFILE_TEST)
public class SletteOppgaveTest {

  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Farskapserklæringen er derfor slettet. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto BARN = henteBarnUtenFnr(5);

  @Autowired
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  private Mapper mapper;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private OppgavebestillingDao oppgavebestillingDao;

  @MockBean
  private KafkaTemplate<Nokkel, Beskjed> beskjedkoe;

  @MockBean
  private KafkaTemplate<Nokkel, Done> ferdigkoe;

  private SletteOppgave sletteOppgave;


  @BeforeEach
  void setup() {

    MockitoAnnotations.openMocks(this); //without this you will get NPE

    // Bønnen sletteOppgave er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    sletteOppgave = SletteOppgave.builder()
        .persistenceService(persistenceService)
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalEgenskaper(farskapsportalEgenskaper)
        .build();
  }

  @Test
  void skalSletteUtloeptOppgaveOgVarsleMorDersomFarIkkeSignererInnenFristen() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(mapper.toEntity(farskapserklaeringSomVenterPaaFarsSignatur));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    // Setter signeringstidspunkt til utenfor levetiden til oppgaven
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager()));
    var lagretFarskapserklaering = persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    var lagretOppgavebestilling = oppgavebestillingDao.save(Oppgavebestilling.builder()
        .eventId(UUID.randomUUID().toString())
        .forelder(lagretFarskapserklaering.getFar())
        .farskapserklaering(lagretFarskapserklaering)
        .opprettet(LocalDateTime.now())
        .build());

    var ferdignoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig()), ferdignoekkelfanger.capture(), ferdigfanger.capture());

    verify(beskjedkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), beskjednoekkelfanger.capture(), beskjedfanger.capture());

    var ferdignokkel = ferdignoekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    var beskjednoekkel = beskjednoekkelfanger.getAllValues().get(0);
    var beskjed = beskjedfanger.getAllValues().get(0);

    var beskjedMorSynligFremTilDato = Instant.ofEpochMilli(beskjed.getSynligFremTil()).atZone(ZoneId.systemDefault()).toLocalDate();

    var oppdatertOppgavebestilling = oppgavebestillingDao.findById(lagretOppgavebestilling.getId());

    assertAll(
        () -> assertThat(ferdignokkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(ferdig.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(ferdig.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThan(ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(5).toEpochSecond()),
        () -> assertThat(beskjednoekkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalEgenskaper.getUrl()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(beskjed.getTekst()).isEqualTo(MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE),
        () -> assertThat(beskjed.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedMorSynligFremTilDato)
            .isEqualTo(LocalDate.now().plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder())),
        () -> assertThat(oppdatertOppgavebestilling.get().getFerdigstilt()).isNotNull()
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
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(mapper.toEntity(farskapserklaeringSomVenterPaaFarsSignatur));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    var lagretOppgavebestilling = oppgavebestillingDao.save(Oppgavebestilling.builder()
        .eventId(UUID.randomUUID().toString())
        .forelder(farskapserklaering.getFar())
        .farskapserklaering(farskapserklaering)
        .opprettet(LocalDateTime.now())
        .build());

    // Setter signeringstidspunkt til utenfor levetiden til oppgaven
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager() - 5));
    persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    verify(ferdigkoe, times(0))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig()), any(Nokkel.class), any(Done.class));

    var oppgavebestillingEtterSlettforsoek = oppgavebestillingDao.findById(lagretOppgavebestilling.getId());

    assertThat(oppgavebestillingEtterSlettforsoek.get().getFerdigstilt()).isNull();
  }


  @Test
  void skalIkkeOppdatereOppgavebestillingDersomFerdigkallFeiler() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(mapper.toEntity(farskapserklaeringSomVenterPaaFarsSignatur));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    var lagretOppgavebestilling = oppgavebestillingDao.save(Oppgavebestilling.builder()
        .eventId(UUID.randomUUID().toString())
        .forelder(farskapserklaering.getFar())
        .farskapserklaering(farskapserklaering)
        .opprettet(LocalDateTime.now())
        .build());

    // Setter signeringstidspunkt til innenfor levetiden til oppgaven
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager() + 5));
    persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    doThrow(KafkaException.class).when(ferdigkoe).send(anyString(), any(Nokkel.class), any(Done.class));

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    var oppgavebestillingEtterSlettforsoek = oppgavebestillingDao.findById(lagretOppgavebestilling.getId());
    assertThat(oppgavebestillingEtterSlettforsoek.get().getFerdigstilt()).isNull();
  }
}
