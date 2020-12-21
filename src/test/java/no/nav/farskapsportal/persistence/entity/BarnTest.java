package no.nav.farskapsportal.persistence.entity;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Barn")
@SpringBootTest(classes = Barn.class)
@ActiveProfiles(PROFILE_TEST)
public class BarnTest {

  @Test
  @DisplayName("Barnets termindato skal være representert i streng-versjonen av en barn-instans")
  void barnetsTermindatoSkalVaereRepresentertIStrengversjonenAvEnBarninstans() {

    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();

    assertEquals(
        String.format("Barn knyttet til termindato: %s", barn.getTermindato().toString()),
        barn.toString());
  }
}
