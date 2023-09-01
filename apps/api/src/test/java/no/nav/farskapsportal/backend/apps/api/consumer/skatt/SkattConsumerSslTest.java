package no.nav.farskapsportal.backend.apps.api.consumer.skatt;

import static no.nav.farskapsportal.backend.apps.api.FarskapsportalApiTestConfig.PROFILE_SKATT_SSL_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.config.FarskapsportalApiConfig;
import no.nav.farskapsportal.backend.apps.api.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.apps.api.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.*;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@EnableMockOAuth2Server
@DisplayName("SkattConsumerSslTest")
@ActiveProfiles(PROFILE_SKATT_SSL_TEST)
@SpringBootTest(
    classes = {FarskapsportalApiApplicationLocal.class, FarskapsportalApiConfig.class},
    webEnvironment = WebEnvironment.DEFINED_PORT)
public class SkattConsumerSslTest {

  private @MockBean OAuth2AccessTokenService oAuth2AccessTokenService;
  private @MockBean OAuth2AccessTokenResponse oAuth2AccessTokenResponse;
  private @MockBean no.digipost.signature.client.ClientConfiguration clientConfiguration;
  private @MockBean no.digipost.signature.client.direct.DirectClient directClient;
  private @MockBean PdlApiConsumer pdlApiConsumer;
  private @MockBean OppgaveApiConsumer oppgaveApiConsumer;
  private @MockBean BucketConsumer bucketConsumer;

  @Value("server.port")
  private String port;

  @Autowired
  private @Qualifier("sikret") SkattConsumer skattConsumerSikret;

  @Autowired
  private @Qualifier("usikret") SkattConsumer skattConsumerUsikret;

  @Test
  void skalIkkeKasteExceptionDersomKommunikasjonMotSkattSkjerMedSikretProtokoll() {

    // given
    var innholdPades = "Jeg erklærer farskap til barnet".getBytes(StandardCharsets.UTF_8);
    var innholdXadesFar = "Fars signatur".getBytes(StandardCharsets.UTF_8);
    var innholdXadesMor = "Mors signatur".getBytes(StandardCharsets.UTF_8);

    when(oAuth2AccessTokenService.getAccessToken(any()))
        .thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, null));
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(5));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml(innholdXadesMor);

    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml(innholdXadesFar);

    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    var padesBlob = BlobIdGcp.builder().bucket("padesr").name("fp-1").build();
    farskapserklaering.getDokument().setBlobIdGcp(padesBlob);

    var xadesMorBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-mor").build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setBlobIdGcp(xadesMorBlob);

    var xadesFarBlob = BlobIdGcp.builder().bucket("xades").name("fp-1-xades-far").build();
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setBlobIdGcp(xadesFarBlob);

    when(bucketConsumer.getContentFromBucket(padesBlob)).thenReturn(innholdPades);
    when(bucketConsumer.getContentFromBucket(xadesMorBlob)).thenReturn(innholdXadesMor);
    when(bucketConsumer.getContentFromBucket(xadesFarBlob)).thenReturn(innholdXadesFar);

    // when, then
    Assertions.assertDoesNotThrow(() -> skattConsumerSikret.registrereFarskap(farskapserklaering));
  }

  @Test
  void skalKasteSkattConsumerExceptionDersomKommunikasjonMotSkattSkjerOverUsikretProtokoll() {

    // given
    when(oAuth2AccessTokenService.getAccessToken(any()))
        .thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, null));
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(5));

    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonMor()
        .setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering
        .getDokument()
        .setDokumentinnhold(
            Dokumentinnhold.builder()
                .innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
                .build());

    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    // when, then
    assertThrows(
        SkattConsumerException.class,
        () -> skattConsumerUsikret.registrereFarskap(farskapserklaering));
  }

  public Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(port, "redirect-mor"))
                    .signeringstidspunkt(LocalDateTime.now())
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder().redirectUrl(lageUrl(port, "/redirect-far")).build())
            .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }
}
