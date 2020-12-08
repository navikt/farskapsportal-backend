package no.nav.farskapsportal.consumer.esignering;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import lombok.SneakyThrows;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.consumer.esignering.stub.DifiESignaturStub;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.ForelderDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("DifiESignaturConsumer")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(
    classes = {FarskapsportalApplicationLocal.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 8096)
public class DifiESignaturConsumerTest {

  private static final ForelderDto MOR =
      ForelderDto.builder()
          .foedselsnummer("12345678910")
          .fornavn("Kjøttdeig")
          .etternavn("Hammer")
          .build();
  private static final ForelderDto FAR =
      ForelderDto.builder()
          .foedselsnummer("11111122222")
          .fornavn("Rask")
          .etternavn("Karaffel")
          .build();
  private static final String STATUS_URL =
      "http://localhost:8096/api/12345678910/direct/signature-jobs/1/status";
  private static final String PADES_URL =
      "https://api.signering.posten.no/api/"
          + MOR.getFoedselsnummer()
          + "/direct/signature-jobs/1/pades";
  @Autowired private DifiESignaturConsumer difiESignaturConsumer;
  @Autowired private DifiESignaturStub difiESignaturStub;

  @Nested
  @DisplayName("Teste oppretteSigneringsjobb")
  class OppretteSigneringsJobb {
    @SneakyThrows
    @Test
    @DisplayName("Skal legge til redirect url ved opprettelse av signeringsoppdrag")
    void skalLeggeTilRedirectUrlVedOpprettelseAvSigneringsoppdrag() {

      // given
      var morsRedirectUrl = "https://mors-redirect-url.no/";
      var farsRedirectUrl = "https://fars-redirect-url.no/";
      difiESignaturStub.runOppretteSigneringsjobbStub(STATUS_URL, morsRedirectUrl, farsRedirectUrl);

      var dokument =
          DokumentDto.builder()
              .dokumentnavn("Farskapsportal.pdf")
              .innhold(
                  "Farskapserklæring for barn med termindato...".getBytes(StandardCharsets.UTF_8))
              .dokumentStatusUrl(new URI("https://getstatus.no/"))
              .build();
      var mor =
          ForelderDto.builder()
              .foedselsnummer(MOR.getFoedselsnummer())
              .fornavn(MOR.getFornavn())
              .etternavn(MOR.getEtternavn())
              .build();
      var far =
          ForelderDto.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .fornavn(FAR.getFornavn())
              .etternavn(FAR.getEtternavn())
              .build();

      // when
      difiESignaturConsumer.oppretteSigneringsjobb(dokument, mor, far);

      // then
      assertAll(
          () -> assertNotNull(dokument.getRedirectUrlMor(), "Mors redirectUrl skal være lagt til"),
          () -> assertNotNull(dokument.getRedirectUrlFar(), "Far redirectUrl skal være lagt til"),
          () ->
              assertEquals(
                  morsRedirectUrl,
                  dokument.getRedirectUrlMor().getRedirectUrl().toString(),
                  "Mors redirectUrl er riktig"),
          () ->
              assertEquals(
                  farsRedirectUrl,
                  dokument.getRedirectUrlFar().getRedirectUrl().toString(),
                  "Fars redirectUrl er riktig"));
    }
  }

  @Nested
  @DisplayName("Teste henteDokumentstatusEtterRedirect")
  class HenteDokumentstatusEtterRedirect {

    @Test
    @DisplayName("Skal hente dokumentstatus etter redirect")
    void skalHenteDokumentstatusEtterRedirect() throws URISyntaxException {

      // given
      difiESignaturStub.runGetStatus(STATUS_URL, PADES_URL, MOR.getFoedselsnummer());

      // when
      var dokumentStatusDto =
          difiESignaturConsumer.henteDokumentstatusEtterRedirect(
              "jadda", Set.of(new URI(STATUS_URL)));

      // then
      assertNotNull(dokumentStatusDto, "Retur-objekt skal ikke være null");
      assertEquals(
          STATUS_URL, dokumentStatusDto.getStatuslenke().toString(), "Status-url skal være riktig");
      assertNotNull(dokumentStatusDto.getPadeslenke(), "Pades-url skal være satt");
      assertEquals(
          PADES_URL, dokumentStatusDto.getPadeslenke().toString(), "Pades-url skal være riktig");
    }
  }

  @Nested
  @DisplayName("")
  class Hent {

    @SneakyThrows
    @DisplayName("")
    void test() {
      var t = difiESignaturConsumer.henteSignertDokument(new URI(PADES_URL));
    }
  }
}
