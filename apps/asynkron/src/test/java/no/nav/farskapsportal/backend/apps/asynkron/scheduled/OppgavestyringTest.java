package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Oppgavebestilling;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalAsynkronTestApplication.class)
@ActiveProfiles(PROFILE_TEST)
public class OppgavestyringTest {

  private static final String BRUKERNOTIFIKASJON_TOPIC_FERDIG = "min-side.aapen-brukernotifikasjon-done-v1";
  private static final String GRUPPERINGSID_FARSKAP = "farskap";

  @Autowired
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private OppgavebestillingDao oppgavebestillingDao;

  @Value("${wiremock.server.port}")
  String wiremockPort;

  @MockBean
  private KafkaTemplate<NokkelInput, DoneInput> ferdigkoe;

  private Oppgavestyring oppgavestyring;


  @BeforeEach
  void setup() {

    MockitoAnnotations.openMocks(this); //without this you will get NPE

    // Bønnen er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    oppgavestyring = Oppgavestyring.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .farskapserklaeringDao(farskapserklaeringDao)
        .persistenceService(persistenceService)
        .build();
  }

  @Test
  void skalSletteUtloeptOppgaveDersomFarskapserklaerinErDeaktivert() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var tidspunktFoerTestIEpochMillis = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(5).toInstant().toEpochMilli();

    var deaktivertFarskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    deaktivertFarskapserklaering.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    deaktivertFarskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    deaktivertFarskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now()
            .minusDays(farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager()));
    deaktivertFarskapserklaering.setDeaktivert(LocalDateTime.now());
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(deaktivertFarskapserklaering);

    oppgavebestillingDao.save(Oppgavebestilling.builder()
        .eventId(UUID.randomUUID().toString())
        .forelder(deaktivertFarskapserklaering.getFar())
        .farskapserklaering(farskapserklaering)
        .opprettet(LocalDateTime.now())
        .build());

    var ferdignoekkelfanger = ArgumentCaptor.forClass(NokkelInput.class);
    var ferdigfanger = ArgumentCaptor.forClass(DoneInput.class);

    // when
    oppgavestyring.rydddeISigneringsoppgaver();

    // then
    verify(ferdigkoe, times(1))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_FERDIG), ferdignoekkelfanger.capture(), ferdigfanger.capture());

    var ferdignokkel = ferdignoekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    var oppdatertOppgavebestilling = oppgavebestillingDao.henteAktiveOppgaver(farskapserklaering.getId(),
        farskapserklaering.getFar().getFoedselsnummer());

    assertAll(
        () -> assertThat(ferdignokkel.getGrupperingsId()).isEqualTo(GRUPPERINGSID_FARSKAP),
        () -> assertThat(ferdignokkel.getFodselsnummer()).isEqualTo(farskapserklaering.getFar().getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestIEpochMillis),
        () -> assertThat(oppdatertOppgavebestilling.isEmpty()).isTrue()
    );
  }

  @Test
  void skalSletteUtloeptOppgaveDersomFarHarSignert() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var tidspunktFoerTestIEpochMillis = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(5).toInstant().toEpochMilli();

    var ferdigstiltFarskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    ferdigstiltFarskapserklaering.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 15));
    ferdigstiltFarskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    ferdigstiltFarskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now()
            .minusDays(farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager()));
    ferdigstiltFarskapserklaering.setDeaktivert(null);
    ferdigstiltFarskapserklaering.getDokument().getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(ferdigstiltFarskapserklaering);

    oppgavebestillingDao.save(Oppgavebestilling.builder()
        .eventId(UUID.randomUUID().toString())
        .forelder(ferdigstiltFarskapserklaering.getFar())
        .farskapserklaering(farskapserklaering)
        .opprettet(LocalDateTime.now())
        .build());

    var ferdignoekkelfanger = ArgumentCaptor.forClass(NokkelInput.class);
    var ferdigfanger = ArgumentCaptor.forClass(DoneInput.class);

    // when
    oppgavestyring.rydddeISigneringsoppgaver();

    // then
    verify(ferdigkoe, times(1))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_FERDIG), ferdignoekkelfanger.capture(), ferdigfanger.capture());

    var ferdignokkel = ferdignoekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    var oppdatertOppgavebestilling = oppgavebestillingDao.henteAktiveOppgaver(farskapserklaering.getId(),
        farskapserklaering.getFar().getFoedselsnummer());

    assertAll(
        () -> assertThat(ferdignokkel.getGrupperingsId()).isEqualTo(GRUPPERINGSID_FARSKAP),
        () -> assertThat(ferdignokkel.getFodselsnummer()).isEqualTo(farskapserklaering.getFar().getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestIEpochMillis),
        () -> assertThat(oppdatertOppgavebestilling.isEmpty()).isTrue()
    );
  }

  @Test
  void skalIkkeSletteOppgaveTilFarskapserklaeringSomIkkeErFerdigstilt() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    // Setter signeringstidspunkt til innenfor levetiden til oppgaven
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(
            farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager() - 5));

    // Skal ikke være mulig å signere fremover i tid. Er synlighet for oppgave hentet riktig inn?
    assert (farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt()
        .isBefore(LocalDateTime.now()));

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var lagretOppgavebestilling = oppgavebestillingDao.save(Oppgavebestilling.builder()
        .eventId(UUID.randomUUID().toString())
        .forelder(farskapserklaeringSomVenterPaaFarsSignatur.getFar())
        .farskapserklaering(farskapserklaering)
        .opprettet(LocalDateTime.now())
        .build());

    // when
    oppgavestyring.rydddeISigneringsoppgaver();

    // then
    var oppgavebestillingEtterSlettforsoek = oppgavebestillingDao.findById(lagretOppgavebestilling.getId());

    assertThat(oppgavebestillingEtterSlettforsoek.get().getFerdigstilt()).isNull();
  }

  public Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument = Dokument.builder().navn("farskapserklaering.pdf")
        .signeringsinformasjonMor(
            Signeringsinformasjon.builder().redirectUrl(lageUrl(wiremockPort, "redirect-mor")).signeringstidspunkt(LocalDateTime.now()).build())
        .signeringsinformasjonFar(Signeringsinformasjon.builder().redirectUrl(lageUrl(wiremockPort, "/redirect-far")).build())
        .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }
}
