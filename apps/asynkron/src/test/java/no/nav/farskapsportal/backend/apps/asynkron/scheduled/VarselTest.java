package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalAsynkronTestApplication.class)
@ActiveProfiles(PROFILE_TEST)
public class VarselTest {

  private static final String BRUKERNOTIFIKASJON_TOPIC_BESKJED = "aapen-brukernotifikasjon-nyBeskjed-v1";
  private static final String MELDING_OM_MANGLENDE_SIGNERING = "Aksjon kreves: Farskapserklæring opprettet den %s for barn med %s er ikke ferdigstilt. Våre systemer mangler informasjon om at far har signert. Far må logge inn på Farskapsportal og forsøke å signere eller oppdatere status på ny. Ta kontakt med NAV ved problemer.";

  @Autowired
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  private Varsel varsel;

  @MockBean
  private KafkaTemplate<Nokkel, Beskjed> beskjedkoe;

  @BeforeEach
  void setup() {

    MockitoAnnotations.openMocks(this); //without this you will get NPE

    // Bønnen varsel er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    varsel = Varsel.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .persistenceService(persistenceService)
        .build();
  }

  @Test
  void skalSendeEksterntVarselTilBeggeForeldreneIErklaeringerDerFarHarOppdatertBorSammenInfoMenIkkeSignertErklaeringForUfoedt() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjonOppgaveSynlighetAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    varsel.varsleOmManglendeSigneringsinfo();

    // then
    verify(beskjedkoe, times(2))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED), beskjednoekkelfanger.capture(), beskjedfanger.capture());

    var beskjednoekkel = beskjednoekkelfanger.getAllValues().get(0);
    var beskjed = beskjedfanger.getAllValues().get(0);

    var meldingstekst = String.format(MELDING_OM_MANGLENDE_SIGNERING,
        farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt().toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        "termindato " + farskapserklaering.getBarn().getTermindato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

    assertAll(
        () -> assertThat(beskjednoekkel.getEventId()).isNotNull(),
        () -> assertThat(beskjed.getTekst()).isEqualTo(meldingstekst)
    );
  }

  @Test
  void skalSendeEksterntVarselTilBeggeForeldreneIErklaeringerDerFarHarOppdatertBorSammenInfoMenIkkeSignertErklaeringForNyfoedt() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnMedFnr(LocalDate.now().minusDays(13)));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjonOppgaveSynlighetAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    varsel.varsleOmManglendeSigneringsinfo();

    // then
    verify(beskjedkoe, times(2))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED), beskjednoekkelfanger.capture(), beskjedfanger.capture());

    var beskjednoekkel = beskjednoekkelfanger.getAllValues().get(0);
    var beskjed = beskjedfanger.getAllValues().get(0);

    var meldingstekst = String.format(MELDING_OM_MANGLENDE_SIGNERING,
        farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt().toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), "fødselsnummer " + farskapserklaering.getBarn().getFoedselsnummer());

    assertAll(
        () -> assertThat(beskjednoekkel.getEventId()).isNotNull(),
        () -> assertThat(beskjed.getTekst()).isEqualTo(meldingstekst)
    );
  }

  @Test
  void skalIkkeSendeVarselOmFarskapserklaeringDerFarIkkeHarOppdatertBorSammen() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnMedFnr(LocalDate.now().minusDays(13)));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjonOppgaveSynlighetAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(null);
    persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    varsel.varsleOmManglendeSigneringsinfo();

    // then
    verify(beskjedkoe, times(0))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED), beskjednoekkelfanger.capture(), beskjedfanger.capture());
  }

  @Test
  void skalIkkeSendeVarselForDeaktivertFarskapserklaering() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnMedFnr(LocalDate.now().minusDays(13)));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getBrukernotifikasjonOppgaveSynlighetAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(LocalDateTime.now());
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    varsel.varsleOmManglendeSigneringsinfo();

    // then
    verify(beskjedkoe, times(0))
        .send(eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED), beskjednoekkelfanger.capture(), beskjedfanger.capture());
  }
}
