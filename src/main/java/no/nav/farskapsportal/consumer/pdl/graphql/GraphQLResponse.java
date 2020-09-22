package no.nav.farskapsportal.consumer.pdl.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.Getter;
import no.nav.farskapsportal.consumer.pdl.api.PersonDto;

import java.util.List;

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
