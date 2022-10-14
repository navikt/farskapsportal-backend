package no.nav.farskapsportal.backend.apps.asynkron.scheduled.arkiv;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("ArkivereFarskapserklaeringer")
@DirtiesContext
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = FarskapsportalAsynkronTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ArkivereFarskapserklaeringerTest {

  @MockBean
  private SkattConsumer skattConsumerMock;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private ForelderDao forelderDao;

  @Autowired
  private MeldingsloggDao meldingsloggDao;

  @Value("${wiremock.server.port}")
  String wiremockPort;

  private ArkivereFarskapserklaeringer arkivereFarskapserklaeringer;

  @BeforeEach
  void setup() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // Bønnen arkivereFarskapserklaeringer er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    arkivereFarskapserklaeringer = ArkivereFarskapserklaeringer.builder()
        .skattConsumer(skattConsumerMock)
        .persistenceService(persistenceService).build();
  }

  private Farskapserklaering henteFarskapserklaeringNyfoedtSignertAvMor(String persnrBarn) {
    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnMedFnr(LocalDate.now().minusWeeks(3), persnrBarn));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.setFarBorSammenMedMor(true);
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt(LocalDateTime.now().toString());
    return farskapserklaering;
  }

  @Nested
  @DisplayName("Teste overføring til Skatt")
  class OverfoereTilSkatt {

    @Test
    void skalOppdatereMeldingsloggVedOverfoeringTilSkatt() {

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor("98953");

      farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());

      var lagretSignertFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      assert (lagretSignertFarskapserklaering.getSendtTilSkatt() == null);

      when(skattConsumerMock.registrereFarskap(lagretSignertFarskapserklaering)).thenReturn(LocalDateTime.now());

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretSignertFarskapserklaering.getId());
      var logginnslag = meldingsloggDao.findAll();

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> assertThat(logginnslag.iterator().next().getMeldingsidSkatt()).isEqualTo(oppdatertFarskapserklaering.get().getMeldingsidSkatt()),
          () -> assertThat(logginnslag.iterator().next().getTidspunktForOversendelse().isEqual(oppdatertFarskapserklaering.get().getSendtTilSkatt()))
      );
    }

    @Test
    void skalSetteTidspunktForOverfoeringVedOverfoeringTilSkatt() {

      // given
      Farskapserklaering farskapserklaering1 = henteFarskapserklaeringNyfoedtSignertAvMor("12345");

      var lagretSignertFarskapserklaering1 = persistenceService.lagreNyFarskapserklaering(farskapserklaering1);
      assert (lagretSignertFarskapserklaering1.getSendtTilSkatt() == null);

      when(skattConsumerMock.registrereFarskap(lagretSignertFarskapserklaering1)).thenReturn(LocalDateTime.now().minusMinutes(1));

      Farskapserklaering farskapserklaering2 = henteFarskapserklaeringNyfoedtSignertAvMor("54321");
      var lagretSignertFarskapserklaering2 = persistenceService.lagreNyFarskapserklaering(farskapserklaering2);
      assert (lagretSignertFarskapserklaering2.getSendtTilSkatt() == null);

      when(skattConsumerMock.registrereFarskap(lagretSignertFarskapserklaering2)).thenReturn(LocalDateTime.now());

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var oppdatertFarskapserklaering1 = farskapserklaeringDao.findById(lagretSignertFarskapserklaering1.getId());

      var oppdatertFarskapserklaering2 = farskapserklaeringDao.findById(lagretSignertFarskapserklaering2.getId());

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering1).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering1.get().getSendtTilSkatt()).isNotNull(),
          () -> assertThat(oppdatertFarskapserklaering2).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering2.get().getSendtTilSkatt()).isNotNull()
      );
    }

    @Test
    void skalIkkeOverfoereErklaeringSomIkkeErSignertAvBeggeParter() {

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor("43215");
      farskapserklaering.setMeldingsidSkatt(null);
      farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(null);
      var lagretFarskapserklaeringIkkeSignertAvFar = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      assert (lagretFarskapserklaeringIkkeSignertAvFar.getMeldingsidSkatt() == null);
      assert (lagretFarskapserklaeringIkkeSignertAvFar.getSendtTilSkatt() == null);
      assert (lagretFarskapserklaeringIkkeSignertAvFar.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt() == null);

      var farskapserklaeringSignertAvBeggeParter = henteFarskapserklaeringNyfoedtSignertAvMor("12345");
      farskapserklaeringSignertAvBeggeParter.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      var lagretFarskapserklaeringSignertAvBeggeParter = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSignertAvBeggeParter);
      assert (lagretFarskapserklaeringSignertAvBeggeParter.getSendtTilSkatt() == null);

      when(skattConsumerMock.registrereFarskap(lagretFarskapserklaeringSignertAvBeggeParter)).thenReturn(LocalDateTime.now());

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var farskapserklaeringIkkeSendtTilSkatt = farskapserklaeringDao.findById(lagretFarskapserklaeringIkkeSignertAvFar.getId());
      var farskapserklaeringSendtTilSkatt = farskapserklaeringDao.findById(lagretFarskapserklaeringSignertAvBeggeParter.getId());
      var logginnslag = meldingsloggDao.findAll();

      assertAll(
          () -> assertThat(farskapserklaeringIkkeSendtTilSkatt).isPresent(),
          () -> assertThat(farskapserklaeringIkkeSendtTilSkatt.get().getMeldingsidSkatt()).isNull(),
          () -> assertThat(farskapserklaeringIkkeSendtTilSkatt.get().getSendtTilSkatt()).isNull(),
          () -> assertThat(farskapserklaeringSendtTilSkatt).isPresent(),
          () -> assertThat(logginnslag.iterator()).hasNext(),
          () -> assertThat(logginnslag.iterator().next().getTidspunktForOversendelse())
              .isEqualTo(farskapserklaeringSendtTilSkatt.get().getSendtTilSkatt()),
          () -> assertThat(logginnslag.iterator().next().getMeldingsidSkatt()).isEqualTo(farskapserklaeringSendtTilSkatt.get().getMeldingsidSkatt())
      );
    }

    @Test
    void skalIkkeOverfoereFarskapserklaeringerSomAlleredeErSendtTilSkatt() {

      // given
      var farskapserklaeringAlleredeOverfoert = henteFarskapserklaeringNyfoedtSignertAvMor("12345");
      farskapserklaeringAlleredeOverfoert.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringAlleredeOverfoert.setSendtTilSkatt(LocalDateTime.now());

      var lagretFarskapserklaeringAlleredeOverfoert = persistenceService.lagreNyFarskapserklaering(farskapserklaeringAlleredeOverfoert);

      verify(skattConsumerMock, never()).registrereFarskap(lagretFarskapserklaeringAlleredeOverfoert);

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaeringAlleredeOverfoert.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt()).isNotNull()
      );
    }
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
