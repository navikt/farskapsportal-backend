package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.Arkiv;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DirtiesContext
@EnableMockOAuth2Server
@DisplayName("DeaktivereFarskapserklaeringer")
@ActiveProfiles(PROFILE_TEST)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
    classes = FarskapsportalApiApplicationLocal.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class DeaktivereFarskapserklaeringerTest {

  private @Autowired FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;
  private @Autowired BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @Autowired ForelderDao forelderDao;
  private @Autowired MeldingsloggDao meldingsloggDao;
  private @MockitoBean GcpStorageManager gcpStorageManager;

  @Value("${wiremock.server.port}")
  String wiremockPort;

  private DeaktivereFarskapserklaeringer deaktivereFarskapserklaeringer;

  private Arkiv egenskaperDeaktivere;

  @BeforeEach
  void setup() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // Bønnen dekativereFarskapserklaeringer er kun tilgjengelig for live-profilen for å unngå
    // skedulert trigging av metoden under test.
    deaktivereFarskapserklaeringer =
        DeaktivereFarskapserklaeringer.builder()
            .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
            .egenskaperArkiv(farskapsportalAsynkronEgenskaper.getArkiv())
            .persistenceService(persistenceService)
            .build();

    egenskaperDeaktivere = farskapsportalAsynkronEgenskaper.getArkiv();
  }

  private Farskapserklaering henteFarskapserklaeringNyfoedtSignertAvMor(
      LocalDateTime signeringstidspunktMor, String persnrBarn) {
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnMedFnr(LocalDate.now().minusWeeks(3), persnrBarn));
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(signeringstidspunktMor);
    return farskapserklaering;
  }

  private Farskapserklaering henteFarskapserklaeringUfoedtSignertAvMor(
      LocalDateTime signeringstidspunktMor, int antallDagerTilTermindato) {
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            Barn.builder().termindato(LocalDate.now().plusDays(antallDagerTilTermindato)).build());
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(signeringstidspunktMor);
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
      var farskapserklaering =
          henteFarskapserklaeringNyfoedtSignertAvMor(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidIkkeFerdigstilteSigneringsoppdragIDager() + 1),
              "12345");

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () ->
              assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNotNull());
    }

    @Test
    void
        skalIkkeDeaktivereFarskapserklaeringMedIkkeUtloeptSigneringsoppdragMenSomManglerFarsSignatur() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaeringNyfoedtSignertAvMor(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidIkkeFerdigstilteSigneringsoppdragIDager()),
              "12345");

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull());
    }

    @Test
    void
        skalIkkeDeaktivereFarskapserklaeringMedIkkeUtloeptSigneringsoppdragMenSomManglerFarsSignaturFoer40DagerErGaatt() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      // tester minimumsverdi for levetid til ikke-fullførte signeringsoppdrag
      var farskapserklaering =
          henteFarskapserklaeringNyfoedtSignertAvMor(LocalDateTime.now().minusDays(0), "12345");

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull());
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
      var farskapserklaering =
          henteFarskapserklaeringNyfoedtSignertAvMor(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidIkkeFerdigstilteSigneringsoppdragIDager() + 1),
              "12345");

      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidOversendteFarskapserklaeringerIDager() + 1));
      farskapserklaering.setSendtTilSkatt(
          LocalDateTime.now()
              .minusDays(
                  egenskaperDeaktivere.getLevetidOversendteFarskapserklaeringerIDager() + 1));

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () ->
              assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNotNull());
    }

    @Test
    void
        skalIkkeDeaktivereArkiverteFarskapserklaeringForNyfoedtFoerErklaeringensLevetidHarUtloept() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaeringNyfoedtSignertAvMor(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidOversendteFarskapserklaeringerIDager() - 1),
              "12345");

      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidOversendteFarskapserklaeringerIDager() - 1));
      farskapserklaering.setSendtTilSkatt(
          LocalDateTime.now()
              .minusDays(
                  egenskaperDeaktivere.getLevetidOversendteFarskapserklaeringerIDager() - 1));

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull());
    }

    @Test
    void
        skalIkkeDeaktivereArkiverteFarskapserklaeringForUfoedtFoerXAntallDagerHarPassertEtterTerimindato() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaeringUfoedtSignertAvMor(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidIkkeFerdigstilteSigneringsoppdragIDager() + 1),
              4);

      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidOversendteFarskapserklaeringerIDager() + 1));
      farskapserklaering.setSendtTilSkatt(
          LocalDateTime.now()
              .minusDays(
                  egenskaperDeaktivere.getLevetidOversendteFarskapserklaeringerIDager() + 1));

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull());
    }

    @Test
    void
        skalDeaktivereArkiverteFarskapserklaeringForUfoedtNaarTermindatoErPassertMedEtBestemtAntallDager() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaeringUfoedtSignertAvMor(
              LocalDateTime.now()
                  .minusDays(
                      egenskaperDeaktivere.getLevetidIkkeFerdigstilteSigneringsoppdragIDager() + 1),
              (egenskaperDeaktivere.getLevetidOversendteFarskapserklaeringerIDager() + 1) * -1);

      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now().minusDays(100));
      farskapserklaering.setSendtTilSkatt(LocalDateTime.now().minusDays(100));

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () ->
              assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNotNull());
    }
  }

  @Nested
  class ManglerSignaturMor {

    @Test
    void skalDeaktivereFarskapserklaeringOgSomManglerMorsSignatur() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor(null, "12345");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSendtTilSignering(LocalDateTime.now().minusDays(3));

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () ->
              assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNotNull());
    }

    @Test
    void
        skalIkkeDeaktivereFarskapserklaeringOgSomManglerMorsSignaturMenIkkeHarVaertInaktivLengeNok() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor(null, "12345");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSendtTilSignering(LocalDateTime.now().minusHours(3));

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      deaktivereFarskapserklaeringer.deaktivereFarskapserklaeringer();

      // then
      var farskapserklaeringEtterDeaktiverering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(farskapserklaeringEtterDeaktiverering).isPresent(),
          () -> assertThat(farskapserklaeringEtterDeaktiverering.get().getDeaktivert()).isNull());
    }
  }

  public Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "redirect-mor"))
                    .signeringstidspunkt(LocalDateTime.now())
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "/redirect-far"))
                    .build())
            .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }
}
