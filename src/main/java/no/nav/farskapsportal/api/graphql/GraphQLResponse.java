package no.nav.farskapsportal.api.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Value;
import no.nav.farskapsportal.api.graphql.queries.person.PersonDto;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphQLResponse {
    Data data;
    List<JsonNode> errors;

    @Value
    public static class Data {
        PersonDto hentPerson;
    }
}
