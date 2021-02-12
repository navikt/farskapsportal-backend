package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;
import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LOCAL;

import java.io.ByteArrayInputStream;
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
import no.nav.farskapsportal.gcp.secretmanager.AddSecretVersion;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
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
      //@Value("${sm://projects/virksomhetssertifikat-dev/secrets/test-virksomhetssertifikat-felles_2018-2021}") String sertifikatP12,
      //@Value("${sm://projects/719909854975/secrets/test-virksomhetssertifikat-felles-keystore-jceks_2018-2021}") byte[] sertifikat,
      @Value("${sm://projects/627047445397/secrets/selfsigned-jceks/versions/3}") String sertifikat,
      @Value("${sm://projects/627047445397/secrets/virksomhetssertifikat-test-passord/versions/1}") String sertifikatP12Passord)  {

    log.info("sert-pwd lengde: {}", sertifikatP12Passord.length());

    // TODO: erstatte med @Value("${sm://projects/virksomhetssertifikat-dev/secrets/test-virksomhetssertifikat-felles_2018-2021}")
    sertifikatP12Passord = "safeone";
    return  KeyStoreConfig.fromJavaKeyStore(IOUtils.toInputStream(new String(Base64.decodeBase64(sertifikat)), Charset.defaultCharset()), "nav-gcp", sertifikatP12Passord, sertifikatP12Passord);
  }

  @Bean
  @Profile({PROFILE_LIVE, PROFILE_LOCAL})
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig) {
    return ClientConfiguration.builder(keyStoreConfig).trustStore(Certificates.TEST).serviceUri(ServiceUri.DIFI_TEST).build();
  }

  @Bean
  @Profile(PROFILE_LOCAL)
  public KeyStoreConfig testKeyStoreConfig() throws IOException {
    var classLoader = getClass().getClassLoader();
    var filnavn = "cert.jceks";
    try (InputStream inputStream = classLoader.getResourceAsStream(filnavn)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Fant ikke " + filnavn);
      } else {
        return KeyStoreConfig.fromJavaKeyStore(inputStream, "selfsigned", "ZfCTJVPbnWAkWSeU", "ZfCTJVPbnWAkWSeU");
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

}
