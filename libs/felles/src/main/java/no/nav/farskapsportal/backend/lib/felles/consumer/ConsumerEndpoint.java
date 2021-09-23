package no.nav.farskapsportal.backend.lib.felles.consumer;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ConsumerEndpoint {

  private Map<ConsumerEndpointName, String> endpointMap = new HashMap<>();

  public void addEndpoint(ConsumerEndpointName endpointName, String endpointPath) {
    this.endpointMap.put(endpointName, endpointPath);
  }

  public String retrieveEndpoint(ConsumerEndpointName endpointName) {
    return endpointMap.get(endpointName);
  }
}
