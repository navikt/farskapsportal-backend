package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;
import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LOCAL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.Certificates;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.ServiceUri;
import no.digipost.signature.client.core.Sender;
import no.digipost.signature.client.direct.DirectClient;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.gcp.secretmanager.AccessSecretVersion;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
public class DifiEsigneringConfig {

  @Value("${farskapsportal-api.disable-esignering}")
  private boolean disableEsignering;

  @Bean
  @Profile(PROFILE_LIVE)
  public KeyStoreConfig keyStoreConfig(
      @Value("${sm://projects/627047445397/secrets/virksomhetssertifikat-test-passord/versions/1}") String sertifikatP12Passord,
      @Autowired(required = false) AccessSecretVersion accessSecretVersion) throws IOException {

    log.info("sert-pwd lengde: {}", sertifikatP12Passord.length());

    var projectId = "719909854975";
    var secretName = "test-virksomhetssertifikat-felles-keystore-jceks_2018-2021";
    var secretVersion = "1";
    var secretPayload = accessSecretVersion.accessSecretVersion(projectId, secretName, secretVersion);

    log.info("lengde sertifikat: {}", secretPayload.getData().size());
    var inputStream = new ByteArrayInputStream(secretPayload.getData().toByteArray());

    return KeyStoreConfig.fromJavaKeyStore(inputStream, "nav integrasjonstjenester test (buypass class 3 test4 ca 3)", sertifikatP12Passord, sertifikatP12Passord);
  }

  @Bean
  @Profile({PROFILE_LIVE, PROFILE_LOCAL, PROFILE_INTEGRATION_TEST})
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig, FarskapsportalEgenskaper farskapsportalEgenskaper) {
    return ClientConfiguration.builder(keyStoreConfig).trustStore(Certificates.TEST).serviceUri(ServiceUri.DIFI_TEST)
        .globalSender(new Sender(farskapsportalEgenskaper.getOrgnummerNav())).build();
  }

  @Bean
  public DirectClient directClient(ClientConfiguration clientConfiguration) {
    return new DirectClient(clientConfiguration);
  }

  @Bean
  public DifiESignaturConsumer difiESignaturConsumer(ClientConfiguration clientConfiguration, ModelMapper modelMapper, DirectClient directClient) {
    return new DifiESignaturConsumer(clientConfiguration, modelMapper, directClient, disableEsignering);
  }
}
