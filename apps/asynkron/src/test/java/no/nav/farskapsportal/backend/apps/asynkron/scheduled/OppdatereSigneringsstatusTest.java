package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.api.FarskapsportalApiConsumer;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalAsynkronTestApplication.class)
public class OppdatereSigneringsstatusTest {

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Mock
  private FarskapsportalApiConsumer farskapsportalApiConsumer;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Value("${wiremock.server.port}")
  String wiremockPort;

  private OppdatereSigneringsstatus oppdatereSigneringsstatus;

  @BeforeEach
  void setup() {

    MockitoAnnotations.openMocks(this); //without this you will get NPE

    // Bønnen oppdatereSigneringsstatus er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    oppdatereSigneringsstatus = OppdatereSigneringsstatus.builder()
        .farskapsportalAsynkronEgenskaper(farskapsportalAsynkronEgenskaper)
        .farskapsportalApiConsumer(farskapsportalApiConsumer)
        .persistenceService(persistenceService)
        .build();
  }

  @Test
  void skalBestilleStatusoppdateringForAktivFarskapserklaeringSomManglerFarsSignaturMenHarBorSammenInfo() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now()
            .minusDays(farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonFar().setSendtTilSignering(
        LocalDateTime.now().minusHours(farskapsportalAsynkronEgenskaper.getOppdatereSigneringsstatusMinAntallTimerEtterFarBleSendtTilSignering()));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().setStatusQueryToken("tokenetMorFikkDaHunSignerte");
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);

    // when
    oppdatereSigneringsstatus.oppdatereSigneringsstatus();

    // then
    verify(farskapsportalApiConsumer, times(1)).synkronisereSigneringsstatus(farskapserklaering.getId());
  }

  @Test
  void skalIkkeBestilleStatusoppdateringForAktivFarskapserklaeringDersomFarsForsoekIkkeErGammeltNok() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now()
            .minusDays(farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonFar().setSendtTilSignering(
        LocalDateTime.now().minusHours(farskapsportalAsynkronEgenskaper.getOppdatereSigneringsstatusMinAntallTimerEtterFarBleSendtTilSignering())
            .plusMinutes(2));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().setStatusQueryToken("tokenetMorFikkDaHunSignerte");
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);

    // when
    oppdatereSigneringsstatus.oppdatereSigneringsstatus();

    // then
    verify(farskapsportalApiConsumer, times(0)).synkronisereSigneringsstatus(farskapserklaering.getId());
  }

  @Test
  void skalIkkeBestilleStatusoppdateringForAktivFarskapserklaeringDersomDetIkkeHarGaattNokTidEtterFarBleSendtTilSignerering() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now()
            .minusDays(farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().setStatusQueryToken("tokenetMorFikkDaHunSignerte");
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonFar().setSendtTilSignering(LocalDateTime.now());
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);

    // when
    oppdatereSigneringsstatus.oppdatereSigneringsstatus();

    // then
    verify(farskapsportalApiConsumer, times(0)).synkronisereSigneringsstatus(farskapserklaering.getId());
  }

  @Test
  void skalIkkeBestilleStatusoppdateringForDeaktivertFarskapserklaeringSomManglerFarsSignaturMenHarBorSammenInfo() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now()
            .minusDays(farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().setStatusQueryToken("tokenetMorFikkDaHunSignerte");
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(LocalDateTime.now());
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);

    // when
    oppdatereSigneringsstatus.oppdatereSigneringsstatus();

    // then
    verify(farskapsportalApiConsumer, times(0)).synkronisereSigneringsstatus(farskapserklaering.getId());
  }

  @Test
  void skalIkkeBestilleStatusoppdateringForAktivFarskapserklaeringSomManglerFarsSignaturOgBorSammenInfo() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now()
            .minusDays(farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().setStatusQueryToken("tokenetMorFikkDaHunSignerte");
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(LocalDateTime.now());
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(null);
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);

    // when
    oppdatereSigneringsstatus.oppdatereSigneringsstatus();

    // then
    verify(farskapsportalApiConsumer, times(0)).synkronisereSigneringsstatus(farskapserklaering.getId());
  }

  @Test
  void skalIkkeBestilleStatusoppdateringForAktivFarskapserklaeringSomErSignertAvFar() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalAsynkronEgenskaper.getOppgavestyringsforsinkelse() + 1));
    farskapserklaeringSomManglerSigneringsstatus.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now()
            .minusDays(farskapsportalAsynkronEgenskaper.getFarskapsportalFellesEgenskaper().getBrukernotifikasjon().getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(
        LocalDateTime.now());
    farskapserklaeringSomManglerSigneringsstatus.getDokument().setStatusQueryToken("tokenetMorFikkDaHunSignerte");
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(LocalDateTime.now());
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);

    // when
    oppdatereSigneringsstatus.oppdatereSigneringsstatus();

    // then
    verify(farskapsportalApiConsumer, times(0)).synkronisereSigneringsstatus(farskapserklaering.getId());
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
