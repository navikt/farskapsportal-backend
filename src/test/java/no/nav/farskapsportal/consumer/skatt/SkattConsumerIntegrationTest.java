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
import java.time.ZoneOffset;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.util.Mapper;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_INTEGRATION_TEST)
@SpringBootTest(classes = FarskapsportalApplicationLocal.class)
@AutoConfigureWireMock(port = 8096)
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
    var filnavnFarskapserklaering = "fp-20210428.pdf";

    var farskapserklaering = mapper.toEntity(FARSKAPSERKLAERING);
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
    assertDoesNotThrow(() -> skattConsumer.registrereFarskap(farskapserklaering));
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
