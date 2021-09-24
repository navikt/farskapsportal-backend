package no.nav.farskapsportal.backend.libs.felles.consumer.pdl.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import no.nav.farskapsportal.backend.libs.dto.pdl.PersonDto;

@Data
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLResponse {

  Data data;
  List<JsonNode> errors;

  @lombok.Data
  @Getter
  public static class Data {

    PersonDto hentPerson;
  }
}
