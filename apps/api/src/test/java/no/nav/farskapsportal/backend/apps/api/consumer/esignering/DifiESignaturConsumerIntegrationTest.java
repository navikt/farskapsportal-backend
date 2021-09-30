package no.nav.farskapsportal.backend.apps.api.consumer.esignering;

import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUri;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import no.digipost.signature.client.direct.DirectClient;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplication;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiLocalConfig;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.stub.DifiESignaturStub;
import no.nav.farskapsportal.backend.libs.dto.ForelderDto;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("DifiESignaturConsumer")
@ActiveProfiles(FarskapsportalApiApplication.PROFILE_INTEGRATION_TEST)
@SpringBootTest(classes = FarskapsportalApiApplicationLocal.class)
public class DifiESignaturConsumerIntegrationTest {

  private static final ForelderDto MOR = ForelderDto.builder()
      .foedselsnummer("11029000478")
      .navn(NavnDto.builder().fornavn("Rakrygget").etternavn("Veggpryd").build()).build();

  private static final ForelderDto FAR = ForelderDto.builder()
      .foedselsnummer("11029400522")
      .navn(NavnDto.builder().fornavn("Treig").etternavn("Tranflaske").build()).build();

  @Autowired
  DirectClient directClientMock;

  @Autowired
  FarskapsportalEgenskaper farskapsportalEgenskaper;

  @Autowired
  private DifiESignaturConsumer difiESignaturConsumer;

  @Autowired
  private DifiESignaturStub difiESignaturStub;

  @Nested
  @DisplayName("Teste henteDokumentstatusEtterRedirect")
  class HenteDokumentstatusEtterRedirect {

    @Test
    @DisplayName("Skal hente dokumentstatus etter redirect")
    void skalHenteDokumentstatusEtterRedirect() throws URISyntaxException {

      // given
      // Hente fra GCP-instans
      var statusUrl = "https://api.difitest.signering.posten.no/api/889640782/direct/signature-jobs/59540/status";
      var statusQueryToken = "9RAYkOFvDkP6fMFUKwmRGgxQuHIDRMXcK1t58HDRTns";

      // when
      var dokumentStatusDto = difiESignaturConsumer.henteStatus(statusQueryToken, Set.of(new URI(statusUrl)));

      // then
      assertAll(
          () -> AssertionsForClassTypes.assertThat(dokumentStatusDto.getSignaturer().size()).isEqualTo(2),
          () -> AssertionsForClassTypes.assertThat(dokumentStatusDto.getSignaturer().get(0).getSignatureier()).isEqualTo(MOR.getFoedselsnummer()),
          () -> AssertionsForClassTypes.assertThat(dokumentStatusDto.getSignaturer().get(0).getXadeslenke()).isNotNull(),
          () -> AssertionsForClassTypes.assertThat(dokumentStatusDto.getSignaturer().get(1).getSignatureier()).isEqualTo(FAR.getFoedselsnummer())
      );
    }
  }

  @Nested
  @DisplayName("Hente signert dokument")
  class HenteSignertDokument {

    @Test
    void skalHenteSignertDokumentFraPostenEsignering() throws IOException {

      // given
      ClassLoader classLoader = getClass().getClassLoader();
      var inputStream = classLoader.getResourceAsStream("src/test/resources/__files/Farskapsportal.pdf");
      var originaltInnhold = inputStream.readAllBytes();
      difiESignaturStub.runGetSignedDocument(FarskapsportalApiLocalConfig.PADES);

      // when
      var dokumentinnhold = difiESignaturConsumer.henteSignertDokument(lageUri(FarskapsportalApiLocalConfig.PADES));

      // then
      assertArrayEquals(originaltInnhold, dokumentinnhold);
    }
  }
}
