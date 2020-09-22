package no.nav.farskapsportal.consumer;

import java.util.HashMap;
import java.util.Map;

public class ConsumerEndpoint {

  private Map<ConsumerEndpointName, String> endpointMap = new HashMap<>();

  public void addEndpoint(ConsumerEndpointName endpointName, String endpointPath){
    this.endpointMap.put(endpointName, endpointPath);
  }

  public String retrieveEndpoint(ConsumerEndpointName endpointName) {
    return endpointMap.get(endpointName);
  }
}
