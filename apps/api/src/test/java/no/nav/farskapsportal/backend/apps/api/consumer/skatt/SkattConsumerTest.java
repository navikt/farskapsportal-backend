package no.nav.farskapsportal.backend.apps.api.consumer.skatt;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.exception.Feilkode.DOKUMENT_MANGLER_INNOHLD;
import static no.nav.farskapsportal.backend.libs.felles.exception.Feilkode.XADES_FAR_UTEN_INNHOLD;
import static no.nav.farskapsportal.backend.libs.felles.exception.Feilkode.XADES_MOR_UTEN_INNHOLD;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiTestApplication;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiTestConfig;
import no.nav.farskapsportal.backend.apps.api.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.*;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageWrapper;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
@DirtiesContext
@ComponentScan("no.nav.farskapsportal.backend")
@SpringBootTest(
    classes = {FarskapsportalApiTestApplication.class, FarskapsportalApiTestConfig.class},
    webEnvironment = WebEnvironment.DEFINED_PORT)
public class SkattConsumerTest {

  private @Autowired SkattConsumer skattConsumer;
  private @Autowired ServletWebServerApplicationContext webServerAppCtxt;
  private @MockBean OAuth2AccessTokenService oAuth2AccessTokenService;
  private @MockBean OAuth2AccessTokenResponse oAuth2AccessTokenResponse;
  private @MockBean BucketConsumer bucketConsumer;
  private @MockBean GcpStorageWrapper gcpStorageWrapper;

  @Test
  void skalReturnereTidspunktForOverfoeringDersomRegistreringAvFarskapGaarIgjennomHosSkatt() {

    // given
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(5));
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.setMeldingsidSkatt("123");

    var innholdPades = "Jeg erklærer farskap til barnet".getBytes(StandardCharsets.UTF_8);
    var padesBlob = BlobIdGcp.builder().bucket("padesr").name("fp-1").build();
    farskapserklaering.getDokument().setBlobIdGcp(padesBlob);

    var innholdXadesMor = "Mors signatur".getBytes(StandardCharsets.UTF_8);
    var xadesMorBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-mor").build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setBlobIdGcp(xadesMorBlob);

    var innholdXadesFar = "Fars signatur".getBytes(StandardCharsets.UTF_8);
    var xadesFarBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-far").build();
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setBlobIdGcp(xadesFarBlob);

    when(bucketConsumer.getContentFromBucket(padesBlob)).thenReturn(innholdPades);
    when(bucketConsumer.getContentFromBucket(xadesMorBlob)).thenReturn(innholdXadesMor);
    when(bucketConsumer.getContentFromBucket(xadesFarBlob)).thenReturn(innholdXadesFar);

    when(oAuth2AccessTokenService.getAccessToken(any()))
        .thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, null));

    // when
    var tidspunktForOverfoering = skattConsumer.registrereFarskap(farskapserklaering);

    // then
    assertAll(
        () -> assertThat(tidspunktForOverfoering.isBefore(LocalDateTime.now())),
        () -> assertThat(tidspunktForOverfoering.isAfter(LocalDateTime.now().minusMinutes(5))));
  }

  @Test
  void skalKasteExceptionDersomPdfDokumentetIkkeHarInnhold() {

    // given
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(8));
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.setMeldingsidSkatt("123");

    var padesUtenInnhold = "".getBytes(StandardCharsets.UTF_8);
    var padesBlob = BlobIdGcp.builder().bucket("padesr").name("fp-1").build();
    farskapserklaering.getDokument().setBlobIdGcp(padesBlob);

    var innholdXadesMor = "Mors signatur".getBytes(StandardCharsets.UTF_8);
    var xadesMorBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-mor").build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setBlobIdGcp(xadesMorBlob);

    var innholdXadesFar = "Fars signatur".getBytes(StandardCharsets.UTF_8);
    var xadesFarBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-far").build();
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setBlobIdGcp(xadesFarBlob);

    when(bucketConsumer.getContentFromBucket(padesBlob)).thenReturn(padesUtenInnhold);
    when(bucketConsumer.getContentFromBucket(xadesMorBlob)).thenReturn(innholdXadesMor);
    when(bucketConsumer.getContentFromBucket(xadesFarBlob)).thenReturn(innholdXadesFar);

    // when, then
    var skattConsumerException =
        assertThrows(
            SkattConsumerException.class,
            () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(DOKUMENT_MANGLER_INNOHLD);
  }

  @Test
  void skalKasteExceptionDersomMorsXadesDokumentIkkeHarInnhold() {

    // given
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(8));
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.setMeldingsidSkatt("123");

    var innholdPades = "Jeg erklærer farskap til barnet".getBytes(StandardCharsets.UTF_8);
    var padesBlob = BlobIdGcp.builder().bucket("padesr").name("fp-1").build();
    farskapserklaering.getDokument().setBlobIdGcp(padesBlob);

    var innholdXadesMorUtenInnhold = "".getBytes(StandardCharsets.UTF_8);
    var xadesMorBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-mor").build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setBlobIdGcp(xadesMorBlob);

    var innholdXadesFar = "Fars signatur".getBytes(StandardCharsets.UTF_8);
    var xadesFarBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-far").build();
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setBlobIdGcp(xadesFarBlob);

    when(bucketConsumer.getContentFromBucket(padesBlob)).thenReturn(innholdPades);
    when(bucketConsumer.getContentFromBucket(xadesMorBlob)).thenReturn(innholdXadesMorUtenInnhold);
    when(bucketConsumer.getContentFromBucket(xadesFarBlob)).thenReturn(innholdXadesFar);

    // when, then
    var skattConsumerException =
        assertThrows(
            SkattConsumerException.class,
            () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(XADES_MOR_UTEN_INNHOLD);
  }

  @Test
  void skalKasteExceptionDersomFarsXadesDokumentetIkkeHarInnhold() {

    // given
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(8));
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.setMeldingsidSkatt("123");

    var innholdPades = "Jeg erklærer farskap til barnet".getBytes(StandardCharsets.UTF_8);
    var padesBlob = BlobIdGcp.builder().bucket("padesr").name("fp-1").build();
    farskapserklaering.getDokument().setBlobIdGcp(padesBlob);

    var innholdXadesMor = "Mors signatur".getBytes(StandardCharsets.UTF_8);
    var xadesMorBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-mor").build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setBlobIdGcp(xadesMorBlob);

    var innholdXadesFarUtenInnhold = "".getBytes(StandardCharsets.UTF_8);
    var xadesFarBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-far").build();
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setBlobIdGcp(xadesFarBlob);

    when(bucketConsumer.getContentFromBucket(padesBlob)).thenReturn(innholdPades);
    when(bucketConsumer.getContentFromBucket(xadesMorBlob)).thenReturn(innholdXadesMor);
    when(bucketConsumer.getContentFromBucket(xadesFarBlob)).thenReturn(innholdXadesFarUtenInnhold);

    // when, then
    var skattConsumerException =
        assertThrows(
            SkattConsumerException.class,
            () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(XADES_FAR_UTEN_INNHOLD);
  }

  public Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(
                        lageUrl(
                            Integer.toString(webServerAppCtxt.getWebServer().getPort()),
                            "redirect-mor"))
                    .signeringstidspunkt(LocalDateTime.now())
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder()
                    .redirectUrl(
                        lageUrl(
                            Integer.toString(webServerAppCtxt.getWebServer().getPort()),
                            "/redirect-far"))
                    .build())
            .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }
}
