package no.nav.farskapsportal.backend.lib.felles.consumer.pdl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.nav.farskapsportal.backend.lib.dto.pdl.NavnDto;
import no.nav.farskapsportal.backend.lib.felles.config.FarskapsportalFellesConfig;
import no.nav.farskapsportal.backend.lib.felles.test.stub.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.backend.lib.felles.test.stub.consumer.pdl.stub.HentPersonSubResponse;
import no.nav.farskapsportal.backend.lib.felles.test.stub.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.backend.lib.felles.test.stub.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PdlApiConsumer")
@ActiveProfiles(FarskapsportalFellesConfig.PROFILE_TEST)
@SpringBootTest(classes = {PdlApiConsumer.class, PdlApiStub.class, StsStub.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 8096)
public class PdlApiConsumerTest {

  @Autowired
  private PdlApiConsumer pdlApiConsumer;

  @Autowired
  private PdlApiStub pdlApiStub;

  @Autowired
  private StsStub stsStub;

  @Nested
  @DisplayName("Hente navn")
  class Navn {

    @Test
    @DisplayName("Skal returnere navn til person dersom fødselsnummer eksisterer")
    public void skalReturnereNavnTilPersonDersomFnrEksisterer() {

      // given
      var fnrOppgittFar = "01018512345";
      stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
      var registrertNavn = NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").etternavn("Olsen").build();
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var returnertNavn = pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar);

      // then
      var sammensattNavn = Stream.of(registrertNavn.getFornavn(), registrertNavn.getMellomnavn(), registrertNavn.getEtternavn())
          .filter(s -> s != null && !toString().isEmpty()).collect(
              Collectors.joining(" "));

      assertThat(sammensattNavn).isEqualTo(returnertNavn);
    }

    @Test
    @DisplayName("Skal kaste nullpointerexception dersom fornavn mangler i retur fra PDL")
    public void skalKasteNullpointerExceptionDersomFornavnManglerIReturFraPdl() {

      // given
      var fnrOppgittFar = "01018512345";
      stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
      var registrertNavn = NavnDto.builder().mellomnavn("Parafin").etternavn("Olsen").build();
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when, then
      assertThrows(NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));
    }

    @Test
    @DisplayName("Skal kaste nullpointerexception dersom fornavn mangler i retur fra PDL")
    public void skalKasteNullpointerExceptionDersomEtternavnManglerIReturFraPdl() {

      // given
      var fnrOppgittFar = "01018512345";
      stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
      var registrertNavn = NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").build();
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when, then
      assertThrows(NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));
    }
  }
}
