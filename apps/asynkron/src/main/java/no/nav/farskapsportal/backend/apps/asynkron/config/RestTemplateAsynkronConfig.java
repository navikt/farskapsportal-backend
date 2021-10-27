package no.nav.farskapsportal.backend.apps.asynkron.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig;
import no.nav.farskapsportal.backend.libs.felles.consumer.sts.SecurityTokenServiceConsumer;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Slf4j
@Configuration
@ComponentScan("no.nav.farskapsportal")
public class RestTemplateAsynkronConfig {

  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  public RestTemplateAsynkronConfig(@Autowired FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper) {
    this.farskapsportalAsynkronEgenskaper = farskapsportalAsynkronEgenskaper;
  }

  @Bean
  @Qualifier("journalpostapi")
  @Scope("prototype")
  public HttpHeaderRestTemplate journalpostApiRestTemplate(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Autowired SecurityTokenServiceConsumer securityTokenServiceConsumer) {
    httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter::fetchCorrelationIdForThread);
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION, () -> "Bearer " + securityTokenServiceConsumer
        .hentIdTokenForServicebruker(farskapsportalAsynkronEgenskaper.getSystembrukerBrukernavn(), farskapsportalAsynkronEgenskaper.getSystembrukerPassord()));
    return httpHeaderRestTemplate;
  }

  @Bean
  @Qualifier("skatt")
  public HttpHeaderRestTemplate skattRestTemplate(@Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate, KeyStoreConfig keyStoreConfig)
      throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {

    var socketFactory = new SSLConnectionSocketFactory(new SSLContextBuilder()
        .loadTrustMaterial(null, new TrustAllStrategy())
        .loadKeyMaterial(keyStoreConfig.keyStore, keyStoreConfig.keystorePassword.toCharArray()).build(),
        NoopHostnameVerifier.INSTANCE);

    var httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
        .setMaxConnTotal(farskapsportalAsynkronEgenskaper.getSkatt().getMaksAntallForbindelser())
        .setMaxConnPerRoute(farskapsportalAsynkronEgenskaper.getSkatt().getMaksAntallForbindelserPerRute())
        .build();

    var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(farskapsportalAsynkronEgenskaper.getSkatt().getMaksVentetidLesing());
    requestFactory.setConnectTimeout(farskapsportalAsynkronEgenskaper.getSkatt().getMaksVentetidForbindelse());

    httpHeaderRestTemplate.setRequestFactory(requestFactory);

    return httpHeaderRestTemplate;
  }
}
