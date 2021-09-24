package farskapsportal.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.lib.felles.TestUtils.henteBarnMedFnr;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.JournalpostApiConsumer;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.api.DokumentInfo;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.api.OpprettJournalpostResponse;
import no.nav.farskapsportal.backend.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.asynkron.exception.Feilkode;
import no.nav.farskapsportal.asynkron.exception.JournalpostApiConsumerException;
import no.nav.farskapsportal.asynkron.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.asynkron.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.asynkron.scheduled.ArkivereFarskapserklaeringer;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.felles.TestUtils;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("ArkivereFarskapserklaeringer")
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 8096)
@SpringBootTest
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
      var farskapserklaering = Farskapserklaering.builder().far(TestUtils.FAR).mor(TestUtils.MOR).barn(
          TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
      farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

      farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));

      farskapserklaering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaering.setMeldingsidSkatt("123");
      farskapserklaering.setFarBorSammenMedMor(true);

      var lagretSignertFarskapserklaering = farskapserklaeringDao.save(farskapserklaering);
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
      var farskapserklaering1 = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR).barn(
          TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
      farskapserklaering1.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaering1.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaering1.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaering1.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaering1.setFarBorSammenMedMor(true);
      farskapserklaering1.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaering1.setMeldingsidSkatt("1234");

      var lagretSignertFarskapserklaering1 = farskapserklaeringDao.save(farskapserklaering1);
      assert (lagretSignertFarskapserklaering1.getSendtTilSkatt() == null);

      when(skattConsumerMock.registrereFarskap(lagretSignertFarskapserklaering1)).thenReturn(LocalDateTime.now().minusMinutes(1));

      var farskapserklaering2 = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR).barn(
          TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "22222")).build();
      farskapserklaering2.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaering2.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

      farskapserklaering2.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaering2.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaering2.setFarBorSammenMedMor(true);
      farskapserklaering2.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

      farskapserklaering2.setMeldingsidSkatt("2345");
      var lagretSignertFarskapserklaering2 = farskapserklaeringDao.save(farskapserklaering2);
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
      var farskapserklaeringIkkeSignertAvFar = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
      farskapserklaeringIkkeSignertAvFar.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      var lagretFarskapserklaeringIkkeSignertAvFar = farskapserklaeringDao.save(farskapserklaeringIkkeSignertAvFar);
      assert (lagretFarskapserklaeringIkkeSignertAvFar.getSendtTilSkatt() == null);

      var farskapserklaeringSignertAvBeggeParter = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "22222")).build();
      farskapserklaeringSignertAvBeggeParter.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringSignertAvBeggeParter.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringSignertAvBeggeParter.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringSignertAvBeggeParter.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringSignertAvBeggeParter.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringSignertAvBeggeParter.setFarBorSammenMedMor(true);
      farskapserklaeringSignertAvBeggeParter.setMeldingsidSkatt("2345");
      var lagretFarskapserklaeringSignertAvBeggeParter = farskapserklaeringDao.save(farskapserklaeringSignertAvBeggeParter);
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
      var farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.setFarBorSammenMedMor(true);
      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.setMeldingsidSkatt("1234");
      farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.setSendtTilJoark(LocalDateTime.now().minusMinutes(10));

      assert (farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getSendtTilSkatt() == null);
      assert (farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt.getSendtTilJoark() != null);

      var lagretFarskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt = farskapserklaeringDao.save(farskapserklaeringSomErSendtTilJoarkMenIkkeTilSkatt);

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
      var farskapserklaeringAlleredeOverfoert = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
      farskapserklaeringAlleredeOverfoert.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
      farskapserklaeringAlleredeOverfoert.getDokument().getSigneringsinformasjonMor()
          .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringAlleredeOverfoert.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringAlleredeOverfoert.getDokument().getSigneringsinformasjonFar()
          .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
      farskapserklaeringAlleredeOverfoert.setFarBorSammenMedMor(false);
      farskapserklaeringAlleredeOverfoert.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringAlleredeOverfoert.setMeldingsidSkatt("1234");
      farskapserklaeringAlleredeOverfoert.setSendtTilSkatt(LocalDateTime.now());
      farskapserklaeringAlleredeOverfoert.setSendtTilJoark(LocalDateTime.now());

      var lagretFarskapserklaeringAlleredeOverfoert = farskapserklaeringDao.save(farskapserklaeringAlleredeOverfoert);

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
      var farskapserklaeringTilSkattOgJoark = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
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

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaeringTilSkattOgJoark);

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
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isAfter(arkivertFarskapserklaering.get().getSendtTilSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getJoarkJournalpostId()).isEqualTo(jpId)
      );
    }

    @Test
    void skalOverfoereFarskapserklaeringerSomGjelderForeldreSomBorSammenTilSkattMenIkkeJoark() {

      // given
      var tidspunktSendtTilSkatt = LocalDateTime.now();
      var farskapserklaeringTilSkattOgJoark = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
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

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaeringTilSkattOgJoark);

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
      var farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
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

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaeringSomErSendtTilSkattMenIkkeTilJoark);

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
              lagretFarskapserklaering.getSendtTilSkatt().withNano(0)),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilJoark()).isAfter(arkivertFarskapserklaering.get().getSendtTilSkatt()),
          () -> assertThat(arkivertFarskapserklaering.get().getJoarkJournalpostId()).isEqualTo(jpId)
      );
    }

    @Test
    void skalOverfoereTilJoarkSelvOmOverfoeringTilSkattFeiler() {

      // given
      var jpId = "123";
      var farskapserklaeringTilSkattOgJoark = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
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

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaeringTilSkattOgJoark);

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

    @Test
    void skalOverfoereTilSkattSelvOmOverfoeringTilJoarkFeiler() {

      // given
      var farskapserklaeringTilSkattOgJoark = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
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

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaeringTilSkattOgJoark);

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
      var farskapserklaeringerAlleredeOverfoert = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
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

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaeringerAlleredeOverfoert);

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
      var farskapserklaeringTilSkattOgJoark = Farskapserklaering.builder().mor(TestUtils.MOR).far(TestUtils.FAR)
          .barn(TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")).build();
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

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaeringTilSkattOgJoark);

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
