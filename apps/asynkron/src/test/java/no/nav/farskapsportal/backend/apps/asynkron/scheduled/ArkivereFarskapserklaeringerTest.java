package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.joark.JournalpostApiConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.exception.JournalpostApiConsumerException;
import no.nav.farskapsportal.backend.apps.asynkron.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.joark.api.DokumentInfo;
import no.nav.farskapsportal.backend.libs.dto.joark.api.OpprettJournalpostResponse;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

  @MockBean
  private JournalpostApiConsumer journalpostApiConsumerMock;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private ForelderDao forelderDao;

  @Autowired
  private MeldingsloggDao meldingsloggDao;

  private ArkivereFarskapserklaeringer arkivereFarskapserklaeringer;

  @BeforeEach
  void setup() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // Bønnen arkivereFarskapserklaeringer er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    arkivereFarskapserklaeringer = ArkivereFarskapserklaeringer.builder()
        .journalpostApiConsumer(journalpostApiConsumerMock)
        .arkivereIJoark(true)
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

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

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
      meldingsloggDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

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
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      meldingsloggDao.deleteAll();

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
    void skalOverfoereFarskapserklaeringTilSkattSomErSendtTilJoarkMenIkkeSkatt() {

      // given
      var farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt = henteFarskapserklaeringNyfoedtSignertAvMor("51432");

      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());

      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.setSendtTilJoark(LocalDateTime.now().minusMinutes(10));

      assert (farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getSendtTilSkatt() == null);
      assert (farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getSendtTilJoark() != null);

      var lagretFarskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt = persistenceService.lagreNyFarskapserklaering(
          farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt);

      when(skattConsumerMock.registrereFarskap(farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt)).thenReturn(LocalDateTime.now());

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getId());

      var logginnslag = meldingsloggDao.findAll();

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark().withNano(0)).isEqualTo(
              lagretFarskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getSendtTilJoark().withNano(0)),
          () -> assertThat(logginnslag.iterator()).hasNext(),
          () -> assertThat(logginnslag.iterator().next().getTidspunktForOversendelse())
              .isEqualTo(arkivertFarskapserklaering.get().getSendtTilSkatt()),
          () -> assertThat(logginnslag.iterator().next().getMeldingsidSkatt()).isEqualTo(arkivertFarskapserklaering.get().getMeldingsidSkatt())
      );
    }

    @Test
    void skalIkkeOverfoereFarskapserklaeringerSomAlleredeErSendtTilSkatt() {

      // given
      var farskapserklaeringAlleredeOverfoert = henteFarskapserklaeringNyfoedtSignertAvMor("12345");

      farskapserklaeringAlleredeOverfoert.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());

      farskapserklaeringAlleredeOverfoert.setSendtTilSkatt(LocalDateTime.now());
      farskapserklaeringAlleredeOverfoert.setSendtTilJoark(LocalDateTime.now());

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

  @Nested
  @DisplayName("Teste overføring til Joark")
  class OverfoereTilJoark {

    @Test
    void skalOverfoereFarskapserklaeringSomGjelderForeldreSomIkkeBorSammenTilSkattOgJoark() {

      // given
      var jpId = "123";
      var tidspunktSendtTilSkatt = LocalDateTime.now();
      var farskapserklaeringTilSkattOgJoark = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111"));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringTilSkattOgJoark.setFarBorSammenMedMor(false);
      farskapserklaeringTilSkattOgJoark.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringTilSkattOgJoark.setMeldingsidSkatt("1234");

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringTilSkattOgJoark);

      when(skattConsumerMock.registrereFarskap(lagretFarskapserklaering)).thenReturn(tidspunktSendtTilSkatt);

      when(journalpostApiConsumerMock.arkivereFarskapserklaering(lagretFarskapserklaering)).thenReturn(
          OpprettJournalpostResponse.builder().journalpostId(jpId).journalpostferdigstilt(true)
              .dokumenter(List.of(DokumentInfo.builder().dokumentInfoId("dok1").build())).build());
      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isEqualTo(lagretFarskapserklaering.getMeldingsidSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt().withNano(0)).isEqualTo(tidspunktSendtTilSkatt.withNano(0)));

      // TODO: JOARK
         /*

          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isAfter(arkivertFarskapserklaering.get().getSendtTilSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getJoarkJournalpostId()).isEqualTo(jpId)
      );
      */

    }

    @Test
    void skalOverfoereFarskapserklaeringerSomGjelderForeldreSomBorSammenTilSkattMenIkkeJoark() {

      // given
      var tidspunktSendtTilSkatt = LocalDateTime.now();
      var farskapserklaeringTilSkattOgJoark = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111"));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringTilSkattOgJoark.setFarBorSammenMedMor(true);
      farskapserklaeringTilSkattOgJoark.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringTilSkattOgJoark.setMeldingsidSkatt("1234");

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringTilSkattOgJoark);

      when(skattConsumerMock.registrereFarskap(lagretFarskapserklaering)).thenReturn(tidspunktSendtTilSkatt);

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isEqualTo(lagretFarskapserklaering.getMeldingsidSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt().withNano(0)).isEqualTo(tidspunktSendtTilSkatt.withNano(0)),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getJoarkJournalpostId()).isNull()
      );
    }

    @Test
    void skalOverfoereFarskapserklaeringerSomTidligereHarBlittOverfoertTilSkattTilJoark() {

      // given
      var jpId = "123";
      var farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark = henteFarskapserklaering(henteForelder(Forelderrolle.MOR),
          henteForelder(Forelderrolle.FAR), henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111"));
      farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark.setFarBorSammenMedMor(false);
      farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark.setMeldingsidSkatt("1234");
      farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark.setSendtTilSkatt(LocalDateTime.now().minusMinutes(30));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark);

      when(journalpostApiConsumerMock.arkivereFarskapserklaering(lagretFarskapserklaering)).thenReturn(
          OpprettJournalpostResponse.builder().journalpostId(jpId).journalpostferdigstilt(true)
              .dokumenter(List.of(DokumentInfo.builder().dokumentInfoId("dok1").build())).build());
      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isEqualTo(lagretFarskapserklaering.getMeldingsidSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt().withNano(0)).isEqualTo(
              lagretFarskapserklaering.getSendtTilSkatt().withNano(0)));
      // TODO: JOARK
      /*
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isAfter(arkivertFarskapserklaering.get().getSendtTilSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getJoarkJournalpostId()).isEqualTo(jpId)
      );*/
    }

    @Disabled("JOARK")
    @Test
    void skalOverfoereTilJoarkSelvOmOverfoeringTilSkattFeiler() {

      // given
      var jpId = "123";
      var farskapserklaeringTilSkattOgJoark = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111"));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringTilSkattOgJoark.setFarBorSammenMedMor(false);
      farskapserklaeringTilSkattOgJoark.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringTilSkattOgJoark.setMeldingsidSkatt("1234");

      assert (farskapserklaeringTilSkattOgJoark.getSendtTilSkatt() == null);
      assert (farskapserklaeringTilSkattOgJoark.getSendtTilJoark() == null);

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringTilSkattOgJoark);

      when(skattConsumerMock.registrereFarskap(lagretFarskapserklaering)).thenThrow(new SkattConsumerException(Feilkode.SKATT_OVERFOERING_FEILET));

      when(journalpostApiConsumerMock.arkivereFarskapserklaering(lagretFarskapserklaering)).thenReturn(
          OpprettJournalpostResponse.builder().journalpostId(jpId).journalpostferdigstilt(true)
              .dokumenter(List.of(DokumentInfo.builder().dokumentInfoId("dok1").build())).build());
      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isEqualTo(lagretFarskapserklaering.getMeldingsidSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt()).isEqualTo(lagretFarskapserklaering.getSendtTilSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isBefore(LocalDateTime.now()),
          () -> assertThat(arkivertFarskapserklaering.get().getJoarkJournalpostId()).isEqualTo(jpId)
      );
    }

    @Disabled("JOARK")
    @Test
    void skalOverfoereTilSkattSelvOmOverfoeringTilJoarkFeiler() {

      // given
      var farskapserklaeringTilSkattOgJoark = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111"));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringTilSkattOgJoark.setFarBorSammenMedMor(false);
      farskapserklaeringTilSkattOgJoark.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringTilSkattOgJoark.setMeldingsidSkatt("1234");

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringTilSkattOgJoark);

      when(skattConsumerMock.registrereFarskap(lagretFarskapserklaering)).thenReturn(LocalDateTime.now());
      when(journalpostApiConsumerMock.arkivereFarskapserklaering(lagretFarskapserklaering)).thenThrow(
          new JournalpostApiConsumerException(Feilkode.JOARK_OVERFOERING_FEILET));

      // when
      var journalpostApiConsumerException = assertThrows(JournalpostApiConsumerException.class,
          () -> arkivereFarskapserklaeringer.vurdereArkivering());

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isEqualTo(lagretFarskapserklaering.getMeldingsidSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt()).isBefore(LocalDateTime.now()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNull(),
          () -> assertThat(journalpostApiConsumerException.getFeilkode()).isEqualTo(Feilkode.JOARK_OVERFOERING_FEILET)
      );
    }

    @Test
    void skalIkkeOverfoereFarskapserklaeringerSomAlleredeHarBlittSendtTilJoark() {

      // given
      var farskapserklaeringerAlleredeOverfoert = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111"));
      farskapserklaeringerAlleredeOverfoert.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringerAlleredeOverfoert.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringerAlleredeOverfoert.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringerAlleredeOverfoert.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringerAlleredeOverfoert.setFarBorSammenMedMor(false);
      farskapserklaeringerAlleredeOverfoert.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringerAlleredeOverfoert.setMeldingsidSkatt("1234");
      farskapserklaeringerAlleredeOverfoert.setSendtTilSkatt(LocalDateTime.now());
      farskapserklaeringerAlleredeOverfoert.setSendtTilJoark(LocalDateTime.now());

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringerAlleredeOverfoert);

      when(skattConsumerMock.registrereFarskap(lagretFarskapserklaering)).thenReturn(LocalDateTime.now());
      verify(journalpostApiConsumerMock, never()).arkivereFarskapserklaering(farskapserklaeringerAlleredeOverfoert);

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isEqualTo(lagretFarskapserklaering.getMeldingsidSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt()).isBefore(LocalDateTime.now()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isBefore(LocalDateTime.now())
      );
    }

    @Test
    void skalIkkeOverfoereFarskapserklaeringerTilJoarkDersomArkivereIJoarkErUsann() {

      // given
      arkivereFarskapserklaeringer = ArkivereFarskapserklaeringer.builder()
          .journalpostApiConsumer(journalpostApiConsumerMock)
          .arkivereIJoark(false)
          .skattConsumer(skattConsumerMock)
          .persistenceService(persistenceService).build();

      var jpId = "123";
      var tidspunktSendtTilSkatt = LocalDateTime.now();
      var farskapserklaeringTilSkattOgJoark = henteFarskapserklaeringNyfoedtSignertAvMor("1234");

      farskapserklaeringTilSkattOgJoark.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringTilSkattOgJoark);

      when(skattConsumerMock.registrereFarskap(lagretFarskapserklaering)).thenReturn(tidspunktSendtTilSkatt);

      when(journalpostApiConsumerMock.arkivereFarskapserklaering(lagretFarskapserklaering)).thenReturn(
          OpprettJournalpostResponse.builder().journalpostId(jpId).journalpostferdigstilt(true)
              .dokumenter(List.of(DokumentInfo.builder().dokumentInfoId("dok1").build())).build());
      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isEqualTo(lagretFarskapserklaering.getMeldingsidSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt().withNano(0)).isEqualTo(tidspunktSendtTilSkatt.withNano(0)),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getJoarkJournalpostId()).isNull());
    }
  }
}
