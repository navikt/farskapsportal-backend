package no.nav.farskapsportal.config;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_LIVE;

import java.nio.charset.Charset;
import no.digipost.signature.client.Certificates;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.ServiceUri;
import no.digipost.signature.client.core.Sender;
import no.digipost.signature.client.direct.DirectClient;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import org.apache.commons.io.IOUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DifiEsigneringConfig {

  private static final String NAV_ORGNR = "123456789";

  @Bean
  @Profile(PROFILE_LIVE)
  public KeyStoreConfig keyStoreConfig(
      @Value("${SERTIFIKAT_ESIGNERING}") String sertifikatEsignering) {
    return KeyStoreConfig.fromOrganizationCertificate(
        IOUtils.toInputStream(sertifikatEsignering, Charset.defaultCharset()), "");
  }

  @Bean
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig) {
    // TODO: Miljøstyre valg av sertifkat og serviceUrl
    return ClientConfiguration.builder(keyStoreConfig)
        .trustStore(Certificates.TEST)
        .serviceUri(ServiceUri.DIFI_TEST)
        .globalSender(new Sender(NAV_ORGNR))
        .build();
  }

  @Bean
  public DirectClient directClient(ClientConfiguration clientConfiguration) {
    return new DirectClient(clientConfiguration);
  }

  @Bean
  public DifiESignaturConsumer difiESignaturConsumer(
      ClientConfiguration clientConfiguration, ModelMapper modelMapper, DirectClient directClient) {
    return new DifiESignaturConsumer(clientConfiguration, modelMapper, directClient);
  }
}
