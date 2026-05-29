package no.nav.farskapsportal.backend.apps.api.consumer.pdl.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import no.nav.farskapsportal.backend.libs.dto.pdl.HentIdenter;
import no.nav.farskapsportal.backend.libs.dto.pdl.PersonDto;

@Data
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLResponse {

  Data data;
  List<GraphQLError> errors;

  @lombok.Data
  @Getter
  public static class Data {

    PersonDto hentPerson;
    HentIdenter hentIdenter;
  }
}
