package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.Certificates;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.ServiceUri;
import no.digipost.signature.client.core.Sender;
import no.digipost.signature.client.direct.DirectClient;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.gcp.secretmanager.AccessSecretVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
public class DifiEsigneringConfig {

  @Bean
  @Profile(PROFILE_LIVE)
  public KeyStoreConfig keyStoreConfig(
      @Value("${sm://projects/627047445397/secrets/virksomhetssertifikat-test-passord/versions/1}") String sertifikatP12Passord,
      @Autowired(required = false) AccessSecretVersion accessSecretVersion) throws IOException {

    log.info("sert-pwd lengde: {}", sertifikatP12Passord.length());

    var projectId = "719909854975";
    var secretName = "test-virksomhetssertifikat-felles-keystore-jceks_2018-2021";
    var secretVersion = "3";
    var secretPayload = accessSecretVersion.accessSecretVersion(projectId, secretName, secretVersion);

    log.info("lengde sertifikat: {}", secretPayload.getData().size());
    var inputStream = new ByteArrayInputStream(secretPayload.getData().toByteArray());

    return KeyStoreConfig
        .fromJavaKeyStore(inputStream, "nav integrasjonstjenester test", sertifikatP12Passord, sertifikatP12Passord);
  }

  @Bean
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig, FarskapsportalEgenskaper farskapsportalEgenskaper) {
    return ClientConfiguration.builder(keyStoreConfig).trustStore(Certificates.TEST).serviceUri(ServiceUri.DIFI_TEST)
        .globalSender(new Sender(farskapsportalEgenskaper.getOrgnummerNav())).build();
  }

  @Bean
  public DirectClient directClient(ClientConfiguration clientConfiguration) {
    return new DirectClient(clientConfiguration);
  }

  @Bean
  public DifiESignaturConsumer difiESignaturConsumer(DirectClient directClient, FarskapsportalEgenskaper farskapsportalEgenskaper) {
    return new DifiESignaturConsumer(directClient, farskapsportalEgenskaper);
  }
}
