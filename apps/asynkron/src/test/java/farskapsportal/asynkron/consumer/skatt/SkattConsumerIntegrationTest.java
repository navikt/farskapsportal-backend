package farskapsportal.asynkron.consumer.skatt;

import static no.nav.farskapsportal.backend.asynkron.FarskapsportalAsynkronApplication.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.felles.TestUtils.FAR;
import static no.nav.farskapsportal.felles.TestUtils.MOR;
import static no.nav.farskapsportal.felles.TestUtils.henteBarnUtenFnr;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import no.nav.farskapsportal.backend.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.backend.lib.entity.Barn;
import no.nav.farskapsportal.backend.lib.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.lib.entity.Farskapserklaering;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_INTEGRATION_TEST)
@SpringBootTest(classes = SkattConsumer.class)
@AutoConfigureWireMock(port = 8096)
public class SkattConsumerIntegrationTest {

  private static final Barn UFOEDT_BARN = henteBarnUtenFnr(17);

  @Autowired
  private SkattConsumer skattConsumer;

  @Test
  void skalIkkeKasteExceptionDersomKommunikasjonMotSkattSkjerMedSikretProtokoll() {

    // given
    var filnavnFarskapserklaering = "fp-20210428.pdf";

    var farskapserklaering = Farskapserklaering.builder().mor(MOR).far(FAR).barn(UFOEDT_BARN).build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());

    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml(readFile("xadesMor.xml"));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml(readFile("xadesMor.xml"));

    var millis = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

    farskapserklaering.setMeldingsidSkatt(Long.toString(millis));
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold(readFile(filnavnFarskapserklaering)).build());

    // when, then
    Assertions.assertDoesNotThrow(() -> skattConsumer.registrereFarskap(farskapserklaering));
  }

  private byte[] readFile(String filnavn) {
    try {
      var classLoader = getClass().getClassLoader();
      File file = new File(classLoader.getResource(filnavn).getFile());

      return Files.readAllBytes(file.toPath());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return null;
  }
}
