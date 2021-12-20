package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("DeaktivereFarskapserklaeringer")
@DirtiesContext
@ActiveProfiles(PROFILE_TEST)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = FarskapsportalAsynkronTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class DeaktivereFarskapserklaeringerTest {

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Autowired
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private ForelderDao forelderDao;

  @Autowired
  private MeldingsloggDao meldingsloggDao;

  private DeaktivereFarskapserklaeringer deaktivereFarskapserklaeringer;

  @BeforeEach
  void setup() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // Bønnen dekativereFarskapserklaeringer er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    deaktivereFarskapserklaeringer = DeaktivereFarskapserklaeringer.builder()
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .persistenceService(persistenceService).build();
  }

  private Farskapserklaering henteFarskapserklaeringNyfoedtSignertAvMor(LocalDateTime signeringstidspunktMor, String persnrBarn) {
    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnMedFnr(LocalDate.now().minusWeeks(3), persnrBarn));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(signeringstidspunktMor);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    return farskapserklaering;
  }

  private Farskapserklaering henteFarskapserklaeringUfoedtSignertAvMor(LocalDateTime signeringstidspunktMor, int antallDagerTilTermindato) {
    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        Barn.builder().termindato(LocalDate.now().plusDays(antallDagerTilTermindato)).build());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(signeringstidspunktMor);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    return farskapserklaering;
  }

  @Nested
  class UtgaattSigneringsoppdrag {

    @Test
    void skalDeaktivereFarskapserklaeringMedUtdatertSigneringsoppdragOgSomManglerFarsSignatur() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor(
          LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidIkkeFerdigstiltSigneringsoppdragIDager() + 1), "12345");

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNotNull()
      );
    }

    @Test
    void skalIkkeDeaktivereFarskapserklaeringMedIkkeUtloeptSigneringsoppdragMenSomManglerFarsSignatur() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor(
          LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidIkkeFerdigstiltSigneringsoppdragIDager()), "12345");

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull()
      );

    }
  }

  @Nested
  class ArkiverteErklaeringer {

    @Test
    void skalDeaktivereArkiverteFarskapserklaeringForNyfoedtEtterErklaeringensLevetidHarUtloept() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor(
          LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidIkkeFerdigstiltSigneringsoppdragIDager() + 1), "12345");

      farskapserklaering.getDokument().getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidOversendteFarskapserklaeringerIDager() + 1));
      farskapserklaering.setSendtTilSkatt(LocalDateTime.now().minusDays(
          farskapsportalFellesEgenskaper.getLevetidOversendteFarskapserklaeringerIDager() + 1));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNotNull()
      );
    }

    @Test
    void skalIkkeDeaktivereArkiverteFarskapserklaeringForNyfoedtFoerErklaeringensLevetidHarUtloept() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor(
          LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidOversendteFarskapserklaeringerIDager() - 1), "12345");

      farskapserklaering.getDokument().getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidOversendteFarskapserklaeringerIDager() - 1));
      farskapserklaering.setSendtTilSkatt(LocalDateTime.now().minusDays(
          farskapsportalFellesEgenskaper.getLevetidOversendteFarskapserklaeringerIDager() - 1));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull()
      );
    }

    @Test
    void skalIkkeDeaktivereArkiverteFarskapserklaeringForUfoedtFoerXAntallDagerHarPassertEtterTerimindato() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaeringUfoedtSignertAvMor(
          LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidIkkeFerdigstiltSigneringsoppdragIDager() + 1), 4);

      farskapserklaering.getDokument().getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidOversendteFarskapserklaeringerIDager() + 1));
      farskapserklaering.setSendtTilSkatt(LocalDateTime.now().minusDays(
          farskapsportalFellesEgenskaper.getLevetidOversendteFarskapserklaeringerIDager() + 1));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull()
      );
    }

    @Test
    void skalDeaktivereArkiverteFarskapserklaeringForUfoedtNaarTermindatoErPassertMedEtBestemtAntallDager() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaeringUfoedtSignertAvMor(
          LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidIkkeFerdigstiltSigneringsoppdragIDager() + 1),
          (farskapsportalFellesEgenskaper.getLevetidOversendteFarskapserklaeringerIDager() + 1) * -1);

      farskapserklaering.getDokument().getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now().minusDays(100));
      farskapserklaering.setSendtTilSkatt(LocalDateTime.now().minusDays(100));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNotNull()
      );
    }
  }
}
