package no.nav.farskapsportal.backend.apps.api.consumer.skatt;

import static no.nav.farskapsportal.backend.apps.api.consumer.skatt.SkattEndpoint.MOTTA_FARSKAPSERKLAERING;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig;
import no.nav.farskapsportal.backend.libs.felles.consumer.ConsumerEndpoint;
import no.nav.farskapsportal.backend.libs.felles.secretmanager.AccessSecretVersion;
import no.nav.farskapsportal.backend.libs.felles.secretmanager.FarskapKeystoreCredentials;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_INTEGRATION_TEST)
@SpringBootTest(classes = {AccessSecretVersion.class, Config.class})
public class SkattConsumerIntegrationTest {

  private static final Forelder MOR = henteForelder(Forelderrolle.MOR);
  private static final Forelder FAR = henteForelder(Forelderrolle.FAR);
  private static final Barn UFOEDT_BARN = henteBarnUtenFnr(17);

  @Value("${url.skatt.base-url}")
  String baseUrl;

  @Value("${url.skatt.registrering-av-farskap}")
  String endpoint;

  @Autowired private ResourceLoader resourceLoader;

  private SkattConsumer getSkattConsumer() {
    var consumerEndpoint = new ConsumerEndpoint();
    consumerEndpoint.addEndpoint(MOTTA_FARSKAPSERKLAERING, baseUrl + endpoint);
    return getSkattConsumer(
        consumerEndpoint, PoolingHttpClientConnectionManagerBuilder.create().build());
  }

  @Test
  void skalIkkeKasteExceptionDersomKommunikasjonMotSkattSkjerMedSikretProtokoll() {

    // given
    var filnavnFarskapserklaering = "farskapserklaering.pdf";

    var farskapserklaering =
        Farskapserklaering.builder()
            .mor(Forelder.builder().foedselsnummer(MOR.getFoedselsnummer()).build())
            .far(Forelder.builder().foedselsnummer(FAR.getFoedselsnummer()).build())
            .barn(Barn.builder().termindato(UFOEDT_BARN.getTermindato()).build())
            .dokument(
                Dokument.builder()
                    .navn(filnavnFarskapserklaering)
                    .dokumentinnhold(
                        Dokumentinnhold.builder()
                            .innhold(readBytes("farskapserklaering-test-20211023.pdf"))
                            .build())
                    .signeringsinformasjonMor(
                        Signeringsinformasjon.builder()
                            .signeringstidspunkt(LocalDateTime.now())
                            .build())
                    .signeringsinformasjonFar(
                        Signeringsinformasjon.builder()
                            .signeringstidspunkt(LocalDateTime.now())
                            .build())
                    .build())
            .build();

    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonMor()
        .setXadesXml(readFile("xades-mor.xml"));
    farskapserklaering
        .getDokument()
        .getSigneringsinformasjonFar()
        .setXadesXml(readFile("xades-far.xml"));

    var millis = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    farskapserklaering.setMeldingsidSkatt(Long.toString(millis));
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    // when, then
    Assertions.assertDoesNotThrow(() -> getSkattConsumer().registrereFarskap(farskapserklaering));
  }

  private byte[] readBytes(String filnavn) {
    try {
      var inputStream = resourceLoader.getClassLoader().getResourceAsStream(filnavn);
      var bytes = inputStream.readAllBytes();
      return bytes;
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return null;
  }

  private byte[] readFile(String filnavn) {
    try {
      var inputStream = resourceLoader.getClassLoader().getResourceAsStream(filnavn);
      var classLoader = getClass().getClassLoader();
      File file = new File(classLoader.getResource(filnavn).getFile());

      return Files.readAllBytes(file.toPath());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  private SkattConsumer getSkattConsumer(
      ConsumerEndpoint consumerEndpoint,
      PoolingHttpClientConnectionManager httpClientConnectionManager) {
    return new SkattConsumer(httpClientConnectionManager, consumerEndpoint);
  }
}

@Slf4j
@Configuration
@Import(FarskapsportalFellesEgenskaper.class)
class Config {

  @Autowired FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Bean
  @Profile(PROFILE_INTEGRATION_TEST)
  public KeyStoreConfig keyStoreConfig(
      @Value("${virksomhetssertifikat.prosjektid}") String virksomhetssertifikatProsjektid,
      @Value("${virksomhetssertifikat.hemmelighetnavn}")
          String virksomhetssertifikatHemmelighetNavn,
      @Value("${virksomhetssertifikat.hemmelighetversjon}")
          String virksomhetssertifikatHemmelighetVersjon,
      @Value("${virksomhetssertifikat.passord.prosjektid}")
          String virksomhetssertifikatPassordProsjektid,
      @Value("${virksomhetssertifikat.passord.hemmelighetnavn}")
          String virksomhetssertifikatPassordHemmelighetNavn,
      @Value("${virksomhetssertifikat.passord.hemmelighetversjon}")
          String virksomhetssertifikatPassordHemmelighetVersjon,
      @Autowired(required = false) AccessSecretVersion accessSecretVersion)
      throws IOException {

    var sertifikatpassord =
        accessSecretVersion
            .accessSecretVersion(
                virksomhetssertifikatPassordProsjektid,
                virksomhetssertifikatPassordHemmelighetNavn,
                virksomhetssertifikatPassordHemmelighetVersjon)
            .getData()
            .toStringUtf8();

    var objectMapper = new ObjectMapper();
    var farskapKeystoreCredentials =
        objectMapper.readValue(sertifikatpassord, FarskapKeystoreCredentials.class);

    log.info("lengde sertifikatpassord {}", farskapKeystoreCredentials.getPassword().length());

    var secretPayload =
        accessSecretVersion.accessSecretVersion(
            virksomhetssertifikatProsjektid,
            virksomhetssertifikatHemmelighetNavn,
            virksomhetssertifikatHemmelighetVersjon);

    log.info("lengde sertifikat: {}", secretPayload.getData().size());
    var inputStream = new ByteArrayInputStream(secretPayload.getData().toByteArray());

    return KeyStoreConfig.fromJavaKeyStore(
        inputStream,
        farskapKeystoreCredentials.getAlias(),
        farskapKeystoreCredentials.getPassword(),
        farskapKeystoreCredentials.getPassword());
  }
}
