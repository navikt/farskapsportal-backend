package no.nav.farskapsportal.backend.apps.api.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import lombok.extern.slf4j.Slf4j;
import no.digipost.signature.client.security.KeyStoreConfig;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.sts.SecurityTokenServiceConsumer;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Slf4j
@Configuration
public class RestTemplateConfig {

  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  public RestTemplateConfig(@Autowired FarskapsportalEgenskaper farskapsportalEgenskaper) {
    this.farskapsportalEgenskaper = farskapsportalEgenskaper;
  }

  @Bean
  @Qualifier("journalpostapi")
  @Scope("prototype")
  public HttpHeaderRestTemplate journalpostApiRestTemplate(
      @Qualifier("base") HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Autowired SecurityTokenServiceConsumer securityTokenServiceConsumer) {
    httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter::fetchCorrelationIdForThread);
    httpHeaderRestTemplate.addHeaderGenerator(HttpHeaders.AUTHORIZATION, () -> "Bearer " + securityTokenServiceConsumer
        .hentIdTokenForServicebruker(farskapsportalEgenskaper.getSystembrukerBrukernavn(), farskapsportalEgenskaper.getSystembrukerPassord()));
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
        .setMaxConnTotal(farskapsportalEgenskaper.getSkatt().getMaksAntallForbindelser())
        .setMaxConnPerRoute(farskapsportalEgenskaper.getSkatt().getMaksAntallForbindelserPerRute())
        .build();

    var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(farskapsportalEgenskaper.getSkatt().getMaksVentetidLesing());
    requestFactory.setConnectTimeout(farskapsportalEgenskaper.getSkatt().getMaksVentetidForbindelse());

    httpHeaderRestTemplate.setRequestFactory(requestFactory);

    return httpHeaderRestTemplate;
  }
}
