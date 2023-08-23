package no.nav.farskapsportal.backend.apps.api.consumer.pdl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.val;
import no.nav.farskapsportal.backend.libs.felles.exception.UnrecoverableException;
import org.springframework.core.io.ClassPathResource;

@UtilityClass
public class PdlApiQuery {

  public static final String HENT_PERSON_BOSTEDSADRESSE =
      readGraphQLQueryFromFile("graphql/hent-person-bostedsadresse-query.graphql");
  public static final String HENT_PERSON_DOEDSFALL =
      readGraphQLQueryFromFile("graphql/hent-person-doedsfall-query.graphql");
  public static final String HENT_PERSON_FOEDSEL =
      readGraphQLQueryFromFile("graphql/hent-person-foedsel-query.graphql");
  public static final String HENT_PERSON_FOLKEREGISTERIDENTIFIKATOR =
      readGraphQLQueryFromFile("graphql/hent-person-folkeregisteridentifikator-query.graphql");
  public static final String HENT_PERSON_FORELDER_BARN_RELASJON =
      readGraphQLQueryFromFile("graphql/hent-person-forelder-barn-relasjon-query.graphql");
  public static final String HENT_PERSON_KJOENN =
      readGraphQLQueryFromFile("graphql/hent-person-kjoenn-query.graphql");
  public static final String HENT_PERSON_NAVN =
      readGraphQLQueryFromFile("graphql/hent-person-navn-query.graphql");
  public static final String HENT_PERSON_SIVILSTAND =
      readGraphQLQueryFromFile("graphql/hent-person-sivilstand-query.graphql");
  public static final String HENT_PERSON_VERGE =
      readGraphQLQueryFromFile("graphql/hent-person-verge-query.graphql");
  public static final String HENT_IDENTER =
      readGraphQLQueryFromFile("graphql/hent-identer.graphql");

  private static String readGraphQLQueryFromFile(String file) {
    val resource = new ClassPathResource(file);
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(resource.getInputStream(), UTF_8))) {
      return reader.lines().collect(Collectors.joining("\n"));
    } catch (IOException e) {
      throw new UnrecoverableException("Failed to read file: " + file, e);
    }
  }
}
