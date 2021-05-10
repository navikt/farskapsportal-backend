package no.nav.farskapsportal.consumer.skatt;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_INTEGRATION_TEST)
@SpringBootTest(classes = FarskapsportalApplicationLocal.class)
public class SkattConsumerIntegrationTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto UFOEDT_BARN = henteBarnUtenFnr(17);
  private static final FarskapserklaeringDto FARSKAPSERKLAERING = henteFarskapserklaeringDto(MOR, FAR, UFOEDT_BARN);

  @Autowired
  private SkattConsumer skattConsumer;

  @Autowired
  private Mapper mapper;

  @Test
  void skalIkkeKasteExceptionDersomKommunikasjonMotSkattSkjerMedSikretProtokoll() {

    // given
    var farskapserklaering = mapper.toEntity(FARSKAPSERKLAERING);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.setMeldingsidSkatt("12345");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold(readFile()).build());

    // when, then
    assertDoesNotThrow(() -> skattConsumer.registrereFarskap(farskapserklaering));
  }

  private byte[] readFile() {
    try {
      var filnavn = "fp-20210428.pdf";
      var classLoader = getClass().getClassLoader();
      File file = new File(classLoader.getResource(filnavn).getFile());

      return Files.readAllBytes(file.toPath());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return null;
  }

}
