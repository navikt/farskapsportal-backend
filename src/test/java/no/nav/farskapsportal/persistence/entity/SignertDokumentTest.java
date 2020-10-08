package no.nav.farskapsportal.persistence.entity;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
  void strengRepresentasjonenAvObjektetSkalViseDokumentnavn() {
    var signertDokument =
        SignertDokument.builder()
            .dokumentnavn("farsSignerteErklaering.pdf")
            .signertDokument("Bekrefter farskap til bar født på denne dato..".getBytes())
            .build();

    var toString = signertDokument.toString();

    assertEquals("Dokumentnavn: " + signertDokument.getDokumentnavn(), toString);
  }
}
