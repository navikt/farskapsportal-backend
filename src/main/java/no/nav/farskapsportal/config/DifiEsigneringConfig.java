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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DifiEsigneringConfig {

  private String miljoe;
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  public DifiEsigneringConfig(@Autowired FarskapsportalEgenskaper farskapsportalEgenskaper, @Value("${NAV_CLUSTER_NAME}") String navClusterName) {
    this.farskapsportalEgenskaper = farskapsportalEgenskaper;
    this.miljoe = navClusterName.equals(NavClusterName.PROD) ? NavClusterName.PROD.toString() : NavClusterName.TEST.toString();
  }

  @Bean
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig) {

    var certificates = miljoe.equals(NavClusterName.TEST) ? Certificates.TEST : Certificates.PRODUCTION;
    var serviceUrl = miljoe.equals(NavClusterName.TEST) ? ServiceUri.DIFI_TEST : ServiceUri.PRODUCTION;

    return ClientConfiguration.builder(keyStoreConfig).trustStore(certificates).serviceUri(serviceUrl)
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

  private enum NavClusterName {
    TEST("dev-gcp"), PROD("prod-gcp");
    String clusterName;

    NavClusterName(String beskrivelse) {
      this.clusterName = beskrivelse;
    }

    public String getClusterName() {
      return this.clusterName;
    }
  }
}
