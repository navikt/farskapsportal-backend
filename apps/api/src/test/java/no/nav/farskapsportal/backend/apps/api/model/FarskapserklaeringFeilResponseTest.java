package no.nav.farskapsportal.backend.apps.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.Optional;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FarskapserklaeringFeilResponseTest {

  private final ObjectMapper objectMapperUtenOptionalStøtte =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  @DisplayName(
      "Skal serialisere FarskapserklæringFeilResponse uten Jackson-feil når antallResterendeForsøk er satt")
  void skalSerialisereResponsMedAntallResterendeForsøk() throws Exception {
    var respons =
        FarskapserklaeringFeilResponse.builder()
            .feilkode(Feilkode.NAVN_STEMMER_IKKE_MED_REGISTER)
            .feilkodebeskrivelse(Feilkode.NAVN_STEMMER_IKKE_MED_REGISTER.getBeskrivelse())
            .antallResterendeForsoek(2)
            .tidspunktForNullstillingAvForsoek(LocalDateTime.now())
            .build();

    // + then
    assertThatCode(() -> objectMapperUtenOptionalStøtte.writeValueAsString(respons))
        .doesNotThrowAnyException();

    var json = objectMapperUtenOptionalStøtte.writeValueAsString(respons);
    assertThat(json).contains("\"antallResterendeForsoek\":2");
  }

  @Test
  @DisplayName(
      "Skal serialisere FarskapserklæringFeilResponse uten Jackson-feil når antallResterendeForsøk er null")
  void skalSerialisereResponsUtenAntallResterendeForsøk() {
    var respons =
        FarskapserklaeringFeilResponse.builder()
            .feilkode(Feilkode.UGYLDIG_FAR)
            .feilkodebeskrivelse(Feilkode.UGYLDIG_FAR.getBeskrivelse())
            .build();

    assertThatCode(() -> objectMapperUtenOptionalStøtte.writeValueAsString(respons))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Skal feile på serialisering når det er en Optional som inputverdi.")
  void skalFeileNårDetBenyttesOptional() {
    var respons = new TestOptionalDeserialisering(Optional.of(1));

    assertThatCode(() -> objectMapperUtenOptionalStøtte.writeValueAsString(respons))
        .doesNotThrowAnyExceptionExcept(InvalidDefinitionException.class);
  }

  class TestOptionalDeserialisering {
    Optional<Integer> antallResterendeForsoek;

    public TestOptionalDeserialisering(Optional<Integer> integer) {
      this.antallResterendeForsoek = integer;
    }
  }
}
