package no.nav.farskapsportal.consumer.pdl;

import lombok.experimental.UtilityClass;
import lombok.val;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import no.nav.farskapsportal.exception.UnrecoverableException;
import org.springframework.core.io.ClassPathResource;


import static java.nio.charset.StandardCharsets.UTF_8;

@UtilityClass
public class PdlApiQuery {
    public static final String HENT_PERSON_KJØNN = readGraphQLQueryFromFile("graphql/hent-person-kjoenn-query.graphql");
    public static final String HENT_PERSON_NAVN = readGraphQLQueryFromFile("graphql/hent-person-navn-query.graphql");

    private static String readGraphQLQueryFromFile(String file) {
        val resource = new ClassPathResource(file);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UnrecoverableException("Failed to read file: " + file, e);
        }
    }
}
