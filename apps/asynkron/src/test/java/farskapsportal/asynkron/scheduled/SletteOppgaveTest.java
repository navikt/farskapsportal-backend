package farskapsportal.asynkron.scheduled;

import static no.nav.farskapsportal.backend.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_TEST;
import static no.nav.farskapsportal.felles.TestUtils.FAR;
import static no.nav.farskapsportal.felles.TestUtils.MOR;
import static no.nav.farskapsportal.felles.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.lib.felles.exception.Feilkode.FANT_IKKE_FARSKAPSERKLAERING;
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
import java.time.ZoneOffset;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.farskapsportal.backend.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.asynkron.scheduled.SletteOppgave;
import no.nav.farskapsportal.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.lib.felles.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.backend.lib.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.lib.entity.Barn;
import no.nav.farskapsportal.backend.lib.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.lib.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.lib.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SletteOppgave")
@SpringBootTest
@ActiveProfiles(PROFILE_TEST)
public class SletteOppgaveTest {

  private static final Barn BARN = henteBarnUtenFnr(5);
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Trykk her for å opprette ny farskapserklæring.";

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
    var tidspunktFoerTestIEpochMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
    farskapserklaeringDao.deleteAll();
    var farskapserklaeringSomVenterPaaFarsSignatur = Farskapserklaering.builder().mor(MOR).far(FAR).barn(BARN).build();
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());

    var farskapserklaering = farskapserklaeringDao.save(farskapserklaeringSomVenterPaaFarsSignatur);
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    // Setter signeringstidspunkt til utenfor levetiden til oppgaven
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager()));
    persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    var ferdignoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getTopicFerdig()), ferdignoekkelfanger.capture(), ferdigfanger.capture());
    verify(beskjedkoe, times(1))
        .send(eq(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), beskjednoekkelfanger.capture(), beskjedfanger.capture());

    var ferdignokkel = ferdignoekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    var beskjednoekkel = beskjednoekkelfanger.getAllValues().get(0);
    var beskjed = beskjedfanger.getAllValues().get(0);

    var beskjedMorSynligFremTilDato = Instant.ofEpochMilli(beskjed.getSynligFremTil()).atZone(ZoneId.systemDefault()).toLocalDate();

    assertAll(
        () -> assertThat(ferdignokkel.getSystembruker()).isEqualTo(farskapsportalAsynkronEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(ferdig.getGrupperingsId()).isEqualTo(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(ferdig.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestIEpochMillis),
        () -> assertThat(beskjednoekkel.getSystembruker()).isEqualTo(farskapsportalAsynkronEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalAsynkronEgenskaper.getUrl()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(beskjed.getTekst()).isEqualTo(MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE),
        () -> assertThat(beskjed.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjedMorSynligFremTilDato)
            .isEqualTo(LocalDate.now().plusMonths(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
    );

    var ressursIkkeFunnetException = assertThrows(RessursIkkeFunnetException.class,
        () -> farskapserklaeringDao.findById(farskapserklaering.getId()));

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
    var farskapserklaeringSomVenterPaaFarsSignatur = Farskapserklaering.builder().mor(MOR).far(FAR).barn(BARN).build();
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());

    var farskapserklaering = farskapserklaeringDao.save(farskapserklaeringSomVenterPaaFarsSignatur);
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    // Setter signeringstidspunkt til innenfor levetiden til oppgaven
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager() - 5));
    persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    verify(ferdigkoe, times(0))
        .send(eq(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon().getTopicFerdig()), any(), any());
  }
}
