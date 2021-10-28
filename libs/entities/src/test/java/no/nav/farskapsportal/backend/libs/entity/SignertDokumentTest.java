package no.nav.farskapsportal.backend.libs.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SignertDokument")
@SpringBootTest(classes = SignertDokumentTest.class)
@ActiveProfiles("test")
public class SignertDokumentTest {

  @Test
  @DisplayName("Streng representasjonen av objektet skal vise dokumentnavn")
  void strengRepresentasjonenAvObjektetSkalViseDokumentnavn() throws URISyntaxException {

    var signertDokument =
        Dokument.builder()
            .navn("farsSignerteErklaering.pdf")
            .build();

    var toString = signertDokument.toString();

    assertEquals("Dokumentnavn: " + signertDokument.getNavn(), toString);
  }
}
