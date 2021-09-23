package no.nav.farskapsportal.backend.lib.entity;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Barn")
@SpringBootTest(classes = Barn.class)
@ActiveProfiles("test")
public class BarnTest {

  @Test
  @DisplayName("Barnets termindato skal være representert i streng-versjonen av en barn-instans")
  void barnetsTermindatoSkalVaereRepresentertIStrengversjonenAvEnBarninstans() {
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    assertEquals(String.format("Barn knyttet til termindato: %s", barn.getTermindato().toString()), barn.toString());
  }

  @Test
  @DisplayName("Skal vise seks første siffer av fødseslnummer ved streng-representasjon")
  void skalViseSeksFoersteSifferAvFoedseslnummerVedStrengrepresentasjon() {
    var foedselsdato = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("ddMMyy"));
    var barn = Barn.builder().foedselsnummer(foedselsdato + "12345").build();
    assertThat(barn.toString()).isEqualTo("Barn med fødselsnummer som starter med: " + foedselsdato);
  }
}
