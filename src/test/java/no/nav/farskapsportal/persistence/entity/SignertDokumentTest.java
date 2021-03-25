package no.nav.farskapsportal.persistence.entity;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SignertDokument")
@SpringBootTest(classes = SignertDokumentTest.class)
@ActiveProfiles(PROFILE_TEST)
public class SignertDokumentTest {

  @Test
  @DisplayName("Streng representasjonen av objektet skal vise dokumentnavn")
  void strengRepresentasjonenAvObjektetSkalViseDokumentnavn() throws URISyntaxException {

    var signertDokument =
        Dokument.builder()
            .dokumentnavn("farsSignerteErklaering.pdf")
            .build();

    var toString = signertDokument.toString();

    assertEquals("Dokumentnavn: " + signertDokument.getDokumentnavn(), toString);
  }
}
