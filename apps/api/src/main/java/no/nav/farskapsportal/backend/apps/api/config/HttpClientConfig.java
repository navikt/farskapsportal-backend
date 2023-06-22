package no.nav.farskapsportal.backend.apps.api.config;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.felles.config.tls.KeyStoreConfig;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class HttpClientConfig {

  @Bean
  public PoolingHttpClientConnectionManager httpClientConnectionManager(
      KeyStoreConfig keyStoreConfig) throws UnrecoverableKeyException, KeyManagementException {
    var sslContextBuilder = new SSLContextBuilder();
    try {
      sslContextBuilder
          .loadTrustMaterial(null, new TrustAllStrategy())
          .loadKeyMaterial(keyStoreConfig.keyStore, keyStoreConfig.keystorePassword.toCharArray())
          .build();
    } catch (NoSuchAlgorithmException | KeyStoreException e) {
      log.error("Pooling Connection Manager Initialisation feilet pga " + e.getMessage(), e);
    }

    SSLConnectionSocketFactory sslsf = null;

    try {
      sslsf = new SSLConnectionSocketFactory(sslContextBuilder.build());
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      log.error("Pooling Connection Manager-initialisering feilet pga " + e.getMessage(), e);
    }

    return PoolingHttpClientConnectionManagerBuilder.create()
        .setSSLSocketFactory(sslsf)
        .setDefaultConnectionConfig(
            ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(30))
                .setConnectTimeout(Timeout.ofSeconds(30))
                .setTimeToLive(TimeValue.ofHours(1))
                .build())
        .setDefaultTlsConfig(
            TlsConfig.custom()
                .setHandshakeTimeout(Timeout.ofSeconds(30))
                .setSupportedProtocols(TLS.V_1_3)
                .build())
        .setMaxConnTotal(100)
        .setMaxConnPerRoute(10)
        .build();
  }
}
