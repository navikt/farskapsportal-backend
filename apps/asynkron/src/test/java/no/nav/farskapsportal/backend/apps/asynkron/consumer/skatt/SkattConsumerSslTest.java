package no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt;

import static no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication.PROFILE_SKATT_SSL_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SkattConsumerSslTest")
@ActiveProfiles(PROFILE_SKATT_SSL_TEST)
@DirtiesContext
@SpringBootTest(classes = FarskapsportalAsynkronTestApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public class SkattConsumerSslTest {

  @MockBean
  private OAuth2AccessTokenService oAuth2AccessTokenService;

  @MockBean
  private OAuth2AccessTokenResponse oAuth2AccessTokenResponse;

  @Autowired
  private ServletWebServerApplicationContext webServerAppCtxt;


  @Nested
  class SikkerProtokoll {

    @Autowired
    private @Qualifier("sikret") SkattConsumer skattConsumerSikret;

    @Test
    void skalIkkeKasteExceptionDersomKommunikasjonMotSkattSkjerMedSikretProtokoll() {

      // given
      when(oAuth2AccessTokenService.getAccessToken(any())).thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, null));
      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));
      farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

      farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));

      farskapserklaering.setMeldingsidSkatt("123");
      farskapserklaering.setSendtTilSkatt(LocalDateTime.now());
      farskapserklaering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

      // when, then
      Assertions.assertDoesNotThrow(() -> skattConsumerSikret.registrereFarskap(farskapserklaering));
    }
  }

  @Nested
  class UsikkerProtokoll {

    @Autowired
    private @Qualifier("usikret") SkattConsumer skattConsumerUsikret;

    @Test
    void skalKasteSkattConsumerExceptionDersomKommunikasjonMotSkattSkjerOverUsikretProtokoll() {

      // given
      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));

      farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

      farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));

      farskapserklaering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

      farskapserklaering.setMeldingsidSkatt("123");
      farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

      // when, then
      assertThrows(SkattConsumerException.class, () -> skattConsumerUsikret.registrereFarskap(farskapserklaering));
    }
  }

  public Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument = Dokument.builder().navn("farskapserklaering.pdf")
        .signeringsinformasjonMor(
            Signeringsinformasjon.builder().redirectUrl(lageUrl(Integer.toString(webServerAppCtxt.getWebServer().getPort()), "redirect-mor"))
                .signeringstidspunkt(LocalDateTime.now()).build())
        .signeringsinformasjonFar(
            Signeringsinformasjon.builder().redirectUrl(lageUrl(Integer.toString(webServerAppCtxt.getWebServer().getPort()), "/redirect-far"))
                .build())
        .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }
}
