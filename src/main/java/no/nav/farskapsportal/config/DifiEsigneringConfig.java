package no.nav.farskapsportal.config;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.Certificates;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.ServiceUri;
import no.digipost.signature.client.core.Sender;
import no.digipost.signature.client.direct.DirectClient;
import no.digipost.signature.client.direct.ExitUrls;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DifiEsigneringConfig {

  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  public DifiEsigneringConfig(@Autowired FarskapsportalEgenskaper farskapsportalEgenskaper) {
    this.farskapsportalEgenskaper = farskapsportalEgenskaper;
  }

  @Bean
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig) {
    return ClientConfiguration.builder(keyStoreConfig).trustStore(Certificates.TEST).serviceUri(ServiceUri.DIFI_TEST)
        .globalSender(new Sender(farskapsportalEgenskaper.getNavOrgnummer())).build();
  }

  @Bean
  public DirectClient directClient(ClientConfiguration clientConfiguration) {
    return new DirectClient(clientConfiguration);
  }

  @Bean
  public DifiESignaturConsumer difiESignaturConsumer(DirectClient directClient) {

    var exitUrls = ExitUrls
        .of(URI.create(farskapsportalEgenskaper.getEsignering().getSuksessUrl()),
            URI.create(farskapsportalEgenskaper.getEsignering().getAvbruttUrl()),
            URI.create(farskapsportalEgenskaper.getEsignering().getFeiletUrl()));

    return new DifiESignaturConsumer(exitUrls, directClient);
  }
}
