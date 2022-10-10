package no.nav.farskapsportal.backend.apps.asynkron.config;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class HttpClientRequestInterceptor implements ClientHttpRequestInterceptor {

  private final String headerName;

  private final String headerValue;

  public HttpClientRequestInterceptor(String headerName, String headerValue) {
    this.headerName = headerName;
    this.headerValue = headerValue;
  }

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    // Add Auth Headers
    request.getHeaders().set(headerName, headerValue);

    // Add logger info settings
    logRequestDetails(request);

    return execution.execute(request, body);
  }

  // Adding custom loggers
  private void logRequestDetails(HttpRequest request) {
    log.info("Request Headers: {}", request.getHeaders());
    log.info("Request Method: {}", request.getMethod());
    log.info("Request URI: {}", request.getURI());
  }
}
