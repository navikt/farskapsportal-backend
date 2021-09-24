package no.nav.farskapsportal.backend.apps.api.config;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_LIVE;

import java.net.URI;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.Certificates;
import no.digipost.signature.client.ClientConfiguration;
import no.digipost.signature.client.ServiceUri;
import no.digipost.signature.client.core.Sender;
import no.digipost.signature.client.direct.DirectClient;
import no.digipost.signature.client.direct.ExitUrls;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
public class DifiEsigneringConfig {

  private final FarskapsportalEgenskaper farskapsportalEgenskaper;
  private final String miljoe;

  public DifiEsigneringConfig(@Autowired FarskapsportalEgenskaper farskapsportalEgenskaper, @Value("${NAIS_CLUSTER_NAME}") String navClusterName) {
    this.farskapsportalEgenskaper = farskapsportalEgenskaper;
    this.miljoe = navClusterName.equals(NavClusterName.PROD.getClusterName()) ? NavClusterName.PROD.toString() : NavClusterName.TEST.toString();
  }

  @Bean
  @Profile(PROFILE_LIVE)
  public ClientConfiguration clientConfiguration(KeyStoreConfig keyStoreConfig) {

    var certificates = miljoe.equals(NavClusterName.TEST.toString()) ? Certificates.TEST : Certificates.PRODUCTION;
    var serviceUrl = miljoe.equals(NavClusterName.TEST.toString()) ? ServiceUri.DIFI_TEST : ServiceUri.PRODUCTION;

    log.info("Kobler opp mot Postens {}-milljø for esignering med service-uri {}.", miljoe.toLowerCase(Locale.ROOT), serviceUrl);

    return ClientConfiguration.builder(keyStoreConfig).trustStore(certificates).serviceUri(serviceUrl)
        .globalSender(new Sender(farskapsportalEgenskaper.getNavOrgnummer())).build();
  }

  @Bean
  public DirectClient directClient(ClientConfiguration clientConfiguration) {
    return new DirectClient(clientConfiguration);
  }

  @Bean
  public DifiESignaturConsumer difiESignaturConsumer(DirectClient directClient, @Autowired PersistenceService persistenceService) {

    var exitUrls = ExitUrls
        .of(URI.create(farskapsportalEgenskaper.getEsignering().getSuksessUrl()),
            URI.create(farskapsportalEgenskaper.getEsignering().getAvbruttUrl()),
            URI.create(farskapsportalEgenskaper.getEsignering().getFeiletUrl()));

    return new DifiESignaturConsumer(exitUrls, directClient, persistenceService);
  }

  private enum NavClusterName {
    TEST("dev-gcp"), PROD("prod-gcp");
    String clusterName;

    NavClusterName(String clusterName) {
      this.clusterName = clusterName;
    }

    public String getClusterName() {
      return this.clusterName;
    }
  }
}
