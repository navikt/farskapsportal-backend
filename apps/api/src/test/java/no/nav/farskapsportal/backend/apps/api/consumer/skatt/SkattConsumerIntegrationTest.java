package no.nav.farskapsportal.backend.apps.api.consumer.skatt;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplication;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.secretmanager.AccessSecretVersion;
import no.nav.farskapsportal.backend.libs.felles.secretmanager.FarskapKeystoreCredentials;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
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
import org.springframework.test.context.ActiveProfiles;


/**
 * Integrasjonstest mot Skatt.
 *
 * Bruker GCP-ressurser.
 *
 * Krever autentisering mot GCP med kontoinfo:
 * >gcloud auth login --update-adc
 *
 * Krever at DB_USERNAME og DB_PASSWORD er satt som miljøvariabler (Intellij run config for SkattConsumerIntegrationTest).
 * 
 */
@Slf4j
@DisplayName("SkattConsumer")
@EnableMockOAuth2Server
@ActiveProfiles(PROFILE_INTEGRATION_TEST)
@SpringBootTest(classes = FarskapsportalApiApplication.class)
public class SkattConsumerIntegrationTest {

  @Value("${url.skatt.base-url}")
  String baseUrl;

  @Value("${url.skatt.registrering-av-farskap}")
  String endpoint;

  private @Autowired SkattConsumer skattConsumer;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;

  /**
   * Tester sending av en farskapserklæring til Skatt
   */
  @Test
  void skalIkkeKasteExceptionDersomKommunikasjonMotSkattSkjerMedSikretProtokoll() {

    // given
    var farskapserklaering = farskapserklaeringDao.findById(1).get();

    var millis = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    farskapserklaering.setMeldingsidSkatt(Long.toString(millis));
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    // when, then
    Assertions.assertDoesNotThrow(() -> skattConsumer.registrereFarskap(farskapserklaering));
  }
}

@Slf4j
@Configuration
@Import(FarskapsportalFellesEgenskaper.class)
class Config {

  @Autowired FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Bean
  @Profile("PROFILE_INTEGRATION_TEST")
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
