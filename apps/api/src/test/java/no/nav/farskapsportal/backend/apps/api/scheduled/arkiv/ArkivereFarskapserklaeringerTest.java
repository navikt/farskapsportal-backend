package no.nav.farskapsportal.backend.apps.api.scheduled.arkiv;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.transaction.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.apps.api.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.*;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.*;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
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

@DirtiesContext
@EnableMockOAuth2Server
@DisplayName("ArkivereFarskapserklaeringer")
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(
    classes = FarskapsportalApiApplicationLocal.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class ArkivereFarskapserklaeringerTest {

  @Value("${wiremock.server.port}")
  String wiremockPort;

  private @MockBean BucketConsumer bucketConsumer;
  private @MockBean SkattConsumer skattConsumerMock;
  private @MockBean DifiESignaturConsumer difiESignaturConsumer;
  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @Autowired ForelderDao forelderDao;
  private @Autowired MeldingsloggDao meldingsloggDao;
  private @Autowired FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;
  private ArkivereFarskapserklaeringer arkivereFarskapserklaeringer;

  @BeforeEach
  void setup() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // Bønnen arkivereFarskapserklaeringer er kun tilgjengelig for live-profilen for å unngå
    // skedulert trigging av metoden under test.
    arkivereFarskapserklaeringer =
        ArkivereFarskapserklaeringer.builder()
            .bucketConsumer(bucketConsumer)
            .skattConsumer(skattConsumerMock)
            .difiESignaturConsumer(difiESignaturConsumer)
            .maksAntallFeilPaaRad(
                farskapsportalAsynkronEgenskaper.getArkiv().getMaksAntallFeilPaaRad())
            .persistenceService(persistenceService)
            .build();
  }

  private Farskapserklaering henteFarskapserklaeringNyfoedtSignertAvMor(String persnrBarn) {
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnMedFnr(LocalDate.now().minusWeeks(3), persnrBarn));
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.setFarBorSammenMedMor(true);
    farskapserklaering.setMeldingsidSkatt(LocalDateTime.now().toString());
    return farskapserklaering;
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
            .statusUrl("http://posten.status.no/")
            .padesUrl("http://url1")
            .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }

  @Nested
  @DisplayName("Teste overføring til Skatt")
  class OverfoereTilSkatt {

    @Test
    void skalOppdatereMeldingsloggVedOverfoeringTilSkatt() {

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor("98953");
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved farskap til dette barnet.".getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);

      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now());

      var lagretSignertFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      assert (lagretSignertFarskapserklaering.getSendtTilSkatt() == null);
      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(
                  farskapsportalAsynkronEgenskaper
                      .getFarskapsportalFellesEgenskaper()
                      .getBucket()
                      .getPadesName())
              .name("fp-" + lagretSignertFarskapserklaering.getId())
              .build();
      when(bucketConsumer.saveContentToBucket(any(), any(), any())).thenReturn(blobIdGcp);

      when(skattConsumerMock.registrereFarskap(lagretSignertFarskapserklaering))
          .thenReturn(LocalDateTime.now());
      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(getStatusDto(farskapserklaering));
      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(farskapserklaeringDokumentinnhold);
      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretSignertFarskapserklaering.getId());
      var logginnslag = meldingsloggDao.findAll();

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertThat(logginnslag.iterator().next().getMeldingsidSkatt())
                  .isEqualTo(oppdatertFarskapserklaering.get().getMeldingsidSkatt()),
          () ->
              assertThat(
                  logginnslag
                      .iterator()
                      .next()
                      .getTidspunktForOversendelse()
                      .isEqual(oppdatertFarskapserklaering.get().getSendtTilSkatt())),
          () ->
              assertThat(oppdatertFarskapserklaering.get().getDokument().getBlobIdGcp())
                  .isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getBlobIdGcp())
                  .isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getBlobIdGcp())
                  .isNotNull(),
          // TODO: Fjerne når bucket-migrering er fullført
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getDokumentinnhold()
                          .getInnhold())
                  .isNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getXadesXml())
                  .isNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getXadesXml())
                  .isNull());
    }

    private DokumentStatusDto getStatusDto(Farskapserklaering farskapserklaering) {
      return DokumentStatusDto.builder()
          .signaturer(
              listOf(
                  SignaturDto.builder()
                      .signatureier(farskapserklaering.getMor().getFoedselsnummer())
                      .build(),
                  SignaturDto.builder()
                      .signatureier(farskapserklaering.getFar().getFoedselsnummer())
                      .build()))
          .padeslenke(tilUri(farskapserklaering.getDokument().getPadesUrl()))
          .build();
    }

    @Test
    void skalSetteTidspunktForOverfoeringVedOverfoeringTilSkatt() throws URISyntaxException {

      // given
      Farskapserklaering farskapserklaering1 = henteFarskapserklaeringNyfoedtSignertAvMor("12345");
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved sannsynligvis farskap til dette barnet"
              .getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);

      var lagretSignertFarskapserklaering1 =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering1);
      assert (lagretSignertFarskapserklaering1.getSendtTilSkatt() == null);

      when(skattConsumerMock.registrereFarskap(lagretSignertFarskapserklaering1))
          .thenReturn(LocalDateTime.now().minusMinutes(1));
      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(getStatusDto(lagretSignertFarskapserklaering1));

      Farskapserklaering farskapserklaering2 = henteFarskapserklaeringNyfoedtSignertAvMor("54321");
      farskapserklaering2.getDokument().setPadesUrl("http://url2");
      var lagretSignertFarskapserklaering2 =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering2);
      assert (lagretSignertFarskapserklaering2.getSendtTilSkatt() == null);

      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(
                  farskapsportalAsynkronEgenskaper
                      .getFarskapsportalFellesEgenskaper()
                      .getBucket()
                      .getPadesName())
              .name("fp-" + farskapserklaering1.getId())
              .build();
      when(bucketConsumer.saveContentToBucket(any(), any(), any())).thenReturn(blobIdGcp);

      when(difiESignaturConsumer.henteSignertDokument(
              new URI(farskapserklaering1.getDokument().getPadesUrl())))
          .thenReturn(farskapserklaeringDokumentinnhold);
      when(difiESignaturConsumer.henteSignertDokument(
              new URI(farskapserklaering2.getDokument().getPadesUrl())))
          .thenReturn((farskapserklaeringDokumentinnhold.toString() + "2").getBytes());
      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(getStatusDto(lagretSignertFarskapserklaering2));
      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);
      when(skattConsumerMock.registrereFarskap(lagretSignertFarskapserklaering2))
          .thenReturn(LocalDateTime.now());

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var oppdatertFarskapserklaering1 =
          farskapserklaeringDao.findById(lagretSignertFarskapserklaering1.getId());

      var oppdatertFarskapserklaering2 =
          farskapserklaeringDao.findById(lagretSignertFarskapserklaering2.getId());

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering1).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering1.get().getSendtTilSkatt()).isNotNull(),
          () -> assertThat(oppdatertFarskapserklaering2).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering2.get().getSendtTilSkatt()).isNotNull(),
          () ->
              assertThat(oppdatertFarskapserklaering1.get().getDokument().getBlobIdGcp())
                  .isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getBlobIdGcp())
                  .isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getBlobIdGcp())
                  .isNotNull(),
          // TODO: Fjerne når bucket-migrering er fullført
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getDokumentinnhold()
                          .getInnhold())
                  .isNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getXadesXml())
                  .isNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getXadesXml())
                  .isNull());
    }

    @Test
    @Transactional
    void skalLagreMeldingsidSkattSelvOmOverfoeringFeiler() throws URISyntaxException {

      // given
      Farskapserklaering farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor("12345");
      farskapserklaering.setMeldingsidSkatt(null);
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved sannsynligvis farskap til dette barnet"
              .getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);

      var lagretSignertFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      assert (lagretSignertFarskapserklaering.getSendtTilSkatt() == null);

      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(
                  farskapsportalAsynkronEgenskaper
                      .getFarskapsportalFellesEgenskaper()
                      .getBucket()
                      .getPadesName())
              .name("fp-" + lagretSignertFarskapserklaering.getId())
              .build();
      when(bucketConsumer.saveContentToBucket(any(), any(), any())).thenReturn(blobIdGcp);

      doThrow(SkattConsumerException.class)
          .when(skattConsumerMock)
          .registrereFarskap(lagretSignertFarskapserklaering);

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(getStatusDto(lagretSignertFarskapserklaering));

      when(difiESignaturConsumer.henteSignertDokument(
              new URI(farskapserklaering.getDokument().getPadesUrl())))
          .thenReturn(farskapserklaeringDokumentinnhold);

      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var oppdatertFarskapserklaering1 =
          farskapserklaeringDao.findById(lagretSignertFarskapserklaering.getId());

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering1).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering1.get().getSendtTilSkatt()).isNull(),
          () -> assertThat(oppdatertFarskapserklaering1.get().getMeldingsidSkatt()).isNotNull(),
          () ->
              assertThat(oppdatertFarskapserklaering1.get().getDokument().getBlobIdGcp())
                  .isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getBlobIdGcp())
                  .isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getBlobIdGcp())
                  .isNotNull(),
          // TODO: Fjerne når bucket-migrering er fullført
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getDokumentinnhold()
                          .getInnhold())
                  .isNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getXadesXml())
                  .isNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering1
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getXadesXml())
                  .isNull());
    }

    @Test
    void skalIkkeFeileMerEnnMaksAntallTillatteGangerPaaRad() throws URISyntaxException {

      // given
      var maksAntallFeilPaaRad =
          farskapsportalAsynkronEgenskaper.getArkiv().getMaksAntallFeilPaaRad();
      for (int i = 0; i < maksAntallFeilPaaRad + 1; i++) {
        Farskapserklaering farskapserklaering =
            henteFarskapserklaeringNyfoedtSignertAvMor("1234" + i);
        farskapserklaering.setMeldingsidSkatt(null);
        farskapserklaering.getDokument().setStatusQueryToken(Integer.toString(i));
        farskapserklaering.setBarn(henteBarnMedFnr(LocalDate.now().minusDays(i)));

        var lagretSignertFarskapserklaering =
            persistenceService.lagreNyFarskapserklaering(farskapserklaering);
        assert (lagretSignertFarskapserklaering.getSendtTilSkatt() == null);

        var blobIdGcp =
            BlobIdGcp.builder()
                .bucket(
                    farskapsportalAsynkronEgenskaper
                        .getFarskapsportalFellesEgenskaper()
                        .getBucket()
                        .getPadesName())
                .name("fp-" + lagretSignertFarskapserklaering.getId())
                .build();
        when(bucketConsumer.saveContentToBucket(any(), any(), any())).thenReturn(blobIdGcp);

        when(difiESignaturConsumer.henteStatus(
                farskapserklaering.getDokument().getStatusQueryToken(),
                farskapserklaering.getDokument().getJobbref(),
                tilUri(farskapserklaering.getDokument().getStatusUrl())))
            .thenReturn(getStatusDto(farskapserklaering));
      }

      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);

      doThrow(SkattConsumerException.class).when(skattConsumerMock).registrereFarskap(any());
      when(difiESignaturConsumer.henteSignertDokument(new URI("http://url1")))
          .thenReturn("en erklæring".getBytes());
      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      verify(skattConsumerMock, times(maksAntallFeilPaaRad)).registrereFarskap(any());
    }

    @Test
    void skalIkkeOverfoereErklaeringSomIkkeErSignertAvBeggeParter() {

      // given
      var farskapserklaering = henteFarskapserklaeringNyfoedtSignertAvMor("43215");
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved farskap til ett par barn".getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);
      farskapserklaering.setMeldingsidSkatt(null);
      farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(null);
      var lagretFarskapserklaeringIkkeSignertAvFar =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      assert (lagretFarskapserklaeringIkkeSignertAvFar.getMeldingsidSkatt() == null);
      assert (lagretFarskapserklaeringIkkeSignertAvFar.getSendtTilSkatt() == null);
      assert (lagretFarskapserklaeringIkkeSignertAvFar
              .getDokument()
              .getSigneringsinformasjonFar()
              .getSigneringstidspunkt()
          == null);

      var farskapserklaeringSignertAvBeggeParter =
          henteFarskapserklaeringNyfoedtSignertAvMor("12345");
      farskapserklaeringSignertAvBeggeParter
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now());
      var lagretFarskapserklaeringSignertAvBeggeParter =
          persistenceService.lagreNyFarskapserklaering(farskapserklaeringSignertAvBeggeParter);
      assert (lagretFarskapserklaeringSignertAvBeggeParter.getSendtTilSkatt() == null);

      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(
                  farskapsportalAsynkronEgenskaper
                      .getFarskapsportalFellesEgenskaper()
                      .getBucket()
                      .getPadesName())
              .name("fp-" + farskapserklaeringSignertAvBeggeParter.getId())
              .build();
      when(bucketConsumer.saveContentToBucket(any(), any(), any())).thenReturn(blobIdGcp);

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(farskapserklaeringDokumentinnhold);
      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);
      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(getStatusDto(lagretFarskapserklaeringSignertAvBeggeParter));
      when(skattConsumerMock.registrereFarskap(lagretFarskapserklaeringSignertAvBeggeParter))
          .thenReturn(LocalDateTime.now());

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var farskapserklaeringIkkeSendtTilSkatt =
          farskapserklaeringDao.findById(lagretFarskapserklaeringIkkeSignertAvFar.getId());
      var farskapserklaeringSendtTilSkatt =
          farskapserklaeringDao.findById(lagretFarskapserklaeringSignertAvBeggeParter.getId());
      var logginnslag = meldingsloggDao.findAll();

      assertAll(
          () -> assertThat(farskapserklaeringIkkeSendtTilSkatt).isPresent(),
          () -> assertThat(farskapserklaeringIkkeSendtTilSkatt.get().getMeldingsidSkatt()).isNull(),
          () -> assertThat(farskapserklaeringIkkeSendtTilSkatt.get().getSendtTilSkatt()).isNull(),
          () -> assertThat(farskapserklaeringSendtTilSkatt).isPresent(),
          () -> assertThat(logginnslag.iterator()).hasNext(),
          () ->
              assertThat(logginnslag.iterator().next().getTidspunktForOversendelse())
                  .isEqualTo(farskapserklaeringSendtTilSkatt.get().getSendtTilSkatt()),
          () ->
              assertThat(logginnslag.iterator().next().getMeldingsidSkatt())
                  .isEqualTo(farskapserklaeringSendtTilSkatt.get().getMeldingsidSkatt()));
    }

    @Test
    void skalIkkeOverfoereFarskapserklaeringerSomAlleredeErSendtTilSkatt() {

      // given
      var farskapserklaeringAlleredeOverfoert = henteFarskapserklaeringNyfoedtSignertAvMor("12345");
      farskapserklaeringAlleredeOverfoert
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringAlleredeOverfoert.setSendtTilSkatt(LocalDateTime.now());

      var lagretFarskapserklaeringAlleredeOverfoert =
          persistenceService.lagreNyFarskapserklaering(farskapserklaeringAlleredeOverfoert);

      verify(skattConsumerMock, never())
          .registrereFarskap(lagretFarskapserklaeringAlleredeOverfoert);

      // when
      arkivereFarskapserklaeringer.vurdereArkivering();

      // then
      var arkivertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaeringAlleredeOverfoert.getId());

      assertAll(
          () -> assertThat(arkivertFarskapserklaering).isPresent(),
          () -> assertThat(arkivertFarskapserklaering.get().getMeldingsidSkatt()).isNotNull(),
          () -> assertThat(arkivertFarskapserklaering.get().getSendtTilSkatt()).isNotNull());
    }
  }
}
