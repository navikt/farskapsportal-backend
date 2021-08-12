package no.nav.farskapsportal.consumer.esignering;

import static no.nav.farskapsportal.FarskapsportalApplication.PROFILE_INTEGRATION_TEST;
import static no.nav.farskapsportal.FarskapsportalLocalConfig.PADES;
import static no.nav.farskapsportal.TestUtils.lageUrl;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import no.digipost.signature.client.direct.DirectClient;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.esignering.stub.DifiESignaturStub;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("DifiESignaturConsumer")
@ActiveProfiles(PROFILE_INTEGRATION_TEST)
@SpringBootTest(classes = FarskapsportalApplicationLocal.class)
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
          () -> assertThat(dokumentStatusDto.getSignaturer().size()).isEqualTo(2),
          () -> assertThat(dokumentStatusDto.getSignaturer().get(0).getSignatureier()).isEqualTo(MOR.getFoedselsnummer()),
          () -> assertThat(dokumentStatusDto.getSignaturer().get(0).getXadeslenke()).isNotNull(),
          () -> assertThat(dokumentStatusDto.getSignaturer().get(1).getSignatureier()).isEqualTo(FAR.getFoedselsnummer())
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
      var inputStream = classLoader.getResourceAsStream("__files/Farskapsportal.pdf");
      var originaltInnhold = inputStream.readAllBytes();
      difiESignaturStub.runGetSignedDocument(PADES);

      // when
      var dokumentinnhold = difiESignaturConsumer.henteSignertDokument(lageUrl(PADES));

      // then
      assertArrayEquals(originaltInnhold, dokumentinnhold);
    }
  }
}
