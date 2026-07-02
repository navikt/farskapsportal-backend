package no.nav.farskapsportal.backend.apps.api.consumer.pdl.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLError {

  private String message;
  private Map<String, Object> extensions;

  public String getCode() {
    if (extensions == null) {
      return "";
    }
    Object code = extensions.get("code");
    return code != null ? code.toString() : "";
  }
}
