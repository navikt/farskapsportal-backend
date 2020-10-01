package no.nav.farskapsportal.consumer.pdl.graphql;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class GraphQLRequest {
  String query;
  String operationName;
  Map<String, Object> variables;
}
