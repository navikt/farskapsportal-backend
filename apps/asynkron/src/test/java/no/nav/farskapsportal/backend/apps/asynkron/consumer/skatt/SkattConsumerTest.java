package no.nav.farskapsportal.backend.apps.asynkron.consumer.skatt;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 0)
@DirtiesContext
@SpringBootTest(classes = FarskapsportalAsynkronTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class SkattConsumerTest {

  @Autowired
  private SkattConsumer skattConsumer;
  @Autowired
  private ServletWebServerApplicationContext webServerAppCtxt;

  @MockBean
  private OAuth2AccessTokenService oAuth2AccessTokenService;

  @MockBean
  private OAuth2AccessTokenResponse oAuth2AccessTokenResponse;

  @Test
  void skalReturnereTidspunktForOverfoeringDersomRegistreringAvFarskapGaarIgjennomHosSkatt() {

    // given
    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");

    when(oAuth2AccessTokenService.getAccessToken(any())).thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, null));

    // when
    var tidspunktForOverfoering = skattConsumer.registrereFarskap(farskapserklaering);

    // then
    assertAll(
        () -> assertThat(tidspunktForOverfoering.isBefore(LocalDateTime.now())),
        () -> assertThat(tidspunktForOverfoering.isAfter(LocalDateTime.now().minusMinutes(5)))
    );
  }

  @Test
  void skalKasteExceptionDersomPdfDokumentetIkkeHarInnhold() {

    // given
    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(8));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("jadda".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(XADES_MOR_UTEN_INNHOLD);
  }

  @Test
  void skalKasteExceptionDersomMorsXadesDokumentIkkeHarInnhold() {

    // given
    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(8));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("jadda".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(XADES_FAR_UTEN_INNHOLD);
  }

  @Test
  void skalKasteExceptionDersomFarsXadesDokumentetIkkeHarInnhold() {

    // given
    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(8));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(DOKUMENT_MANGLER_INNOHLD);
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
