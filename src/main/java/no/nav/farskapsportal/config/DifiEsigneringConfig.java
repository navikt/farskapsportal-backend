package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;

import com.google.cloud.spring.secretmanager.SecretManagerTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.Certificates;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.ServiceUri;
import no.digipost.signature.client.direct.DirectClient;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
public class DifiEsigneringConfig {

  //@Autowired
 // private SecretManagerTemplate secretManagerTemplate;

  @Value("${farskapsportal-api.disable-esignering}")
  private boolean disableEsignering;

  @Bean
  public KeyStoreConfig keyStoreConfig(
      @Value("${sm://projects/virksomhetssertifikat-dev/secrets/test-virksomhetssertifikat-felles_2018-2021}") String sertifikatP12,
      @Value("${sm://projects/627047445397/secrets/virksomhetssertifikat-test-passord/versions/1}") String sertifikatP12Passord) throws IOException {

    if (!disableEsignering) {
      log.info("Oppretter secret..");
     //createSecret("test-cert", sertifikatP12, "farskapsportal-dev-169c");
    }

    return disableEsignering ? testKeyStoreConfig()
        : KeyStoreConfig.fromOrganizationCertificate(IOUtils.toInputStream(sertifikatP12, Charset.defaultCharset()), sertifikatP12Passord);
  }

  @Bean
  @Profile(PROFILE_LIVE)
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig) {
    return ClientConfiguration.builder(keyStoreConfig).trustStore(Certificates.TEST).serviceUri(ServiceUri.DIFI_TEST).build();
  }

  private KeyStoreConfig testKeyStoreConfig() throws IOException {
    var classLoader = getClass().getClassLoader();
    try (InputStream inputStream = classLoader.getResourceAsStream("esigneringkeystore.jceks")) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke esigneringkeystore.jceks");
      } else {
        return KeyStoreConfig.fromJavaKeyStore(inputStream, "selfsigned", "changeit", "changeit");
      }
    }
  }

  @Bean
  public DirectClient directClient(ClientConfiguration clientConfiguration) {
    return new DirectClient(clientConfiguration);
  }

  @Bean
  public DifiESignaturConsumer difiESignaturConsumer(ClientConfiguration clientConfiguration, ModelMapper modelMapper, DirectClient directClient) {
    return new DifiESignaturConsumer(clientConfiguration, modelMapper, directClient, disableEsignering);
  }


  /*
  private void createSecret(String secretId, String secretPayload, String projectId) {
    log.info("lengde sertifikat: {}", secretPayload.length());
    if (StringUtils.isEmpty(projectId)) {
      this.secretManagerTemplate.createSecret(secretId, secretPayload);
    } else {
      this.secretManagerTemplate.createSecret(secretId, secretPayload.getBytes(), projectId);
    }

  }
  */
}
