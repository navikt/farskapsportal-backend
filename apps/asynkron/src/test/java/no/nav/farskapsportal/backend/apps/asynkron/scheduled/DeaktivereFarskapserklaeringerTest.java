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
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("DeaktivereFarskapserklaeringer")
@DirtiesContext
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = FarskapsportalAsynkronTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class DeaktivereFarskapserklaeringerTest {

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

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
    deaktivereFarskapserklaeringer = DeaktivereFarskapserklaeringer.builder().persistenceService(persistenceService).build();
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
    deaktivereFarskapserklaeringer.vurdereDeaktivering();

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
    deaktivereFarskapserklaeringer.vurdereDeaktivering();

    // then
    var farskapserklaeringEtterDeaktiverering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    assertAll(
        () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
        () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull()
    );

  }

  @Test
  void skalIkkeDeaktivereFerdigstiltFarskapserklaering() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // given
    var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor(
        LocalDateTime.now().minusDays(farskapsportalFellesEgenskaper.getLevetidIkkeFerdigstiltSigneringsoppdragIDager() + 1), "12345");

    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("en signatur".getBytes(StandardCharsets.UTF_8));

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

    // when
    deaktivereFarskapserklaeringer.vurdereDeaktivering();

    // then
    var farskapserklaeringEtterDeaktiverering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    assertAll(
        () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
        () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull()
    );
  }
}
