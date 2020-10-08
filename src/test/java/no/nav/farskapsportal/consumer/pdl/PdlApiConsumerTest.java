package no.nav.farskapsportal.consumer.pdl;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSubQuery;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PdlApiConsumer")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(
    classes = {FarskapsportalApplicationLocal.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 8096)
public class PdlApiConsumerTest {

  @Autowired private PdlApiConsumer pdlApiConsumer;

  @Autowired private PdlApiStub pdlApiStub;

  @Autowired private StsStub stsStub;

  @Test
  @DisplayName("Skal hente kjønn hvis person eksisterer")
  public void skalHenteKjoennHvisPersonEksisterer() {

    // given
    var fnrMor = "111222240280";
    var kjoennMor = Kjoenn.KVINNE;
    stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
    List<HentPersonSubQuery> subQueries = List.of(new HentPersonKjoenn(Kjoenn.KVINNE));
    pdlApiStub.runPdlApiHentPersonStub(subQueries);

    // when
    var respons = pdlApiConsumer.henteKjoenn(fnrMor);

    // then
    var returnertKjoenn = respons.getResponseEntity().getBody();
    assertAll(
        () -> assertThat(respons.is2xxSuccessful()),
        () ->
            assertEquals(
                kjoennMor.toString(), returnertKjoenn != null ? returnertKjoenn.name() : null));
  }

  @Test
  @DisplayName("Skal feile dersom informasjon om kjønn mangler")
  public void skalGi404DersomInformasjonOmKjoennMangler() {

    // given
    var fnrMor = "111222240280";
    stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
    List<HentPersonSubQuery> subQueries = List.of(new HentPersonKjoenn(null));
    pdlApiStub.runPdlApiHentPersonStub(subQueries);

    // when
    var respons = pdlApiConsumer.henteKjoenn(fnrMor);

    // then
    var returnertKjoenn = respons.getResponseEntity().getBody();
    assertAll(
        () -> assertEquals(HttpStatus.NOT_FOUND, respons.getResponseEntity().getStatusCode()),
        () -> assertNull(returnertKjoenn));
  }

  @Test
  @DisplayName("Skal returnere navn til person dersom fødselsnummer eksisterer")
  public void skalReturnereNavnTilPersonDersomFnrEksisterer() {

    // given
    var fnrOppgittFar =
        "01018512345() -> assertTrue(registrertNavn.getFornavn().equals(returnertNavn.getFornavn())";
    stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
    var registrertNavn =
        NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").etternavn("Olsen").build();
    List<HentPersonSubQuery> subQueries = List.of(new HentPersonNavn(registrertNavn));
    pdlApiStub.runPdlApiHentPersonStub(subQueries);

    // when
    var respons = pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar);
    var returnertNavn = respons.getResponseEntity().getBody();

    // then
    assertAll(
        () -> assertTrue(respons.getResponseEntity().getStatusCode().is2xxSuccessful()),
        () ->
            assertEquals(
                registrertNavn.getFornavn(),
                returnertNavn != null ? returnertNavn.getFornavn() : null),
        () ->
            assertEquals(
                registrertNavn.getMellomnavn(),
                returnertNavn != null ? returnertNavn.getMellomnavn() : null),
        () ->
            assertEquals(
                registrertNavn.getEtternavn(),
                returnertNavn != null ? returnertNavn.getEtternavn() : null));
  }

  @Test
  @DisplayName("Skal kaste nullpointerexception dersom fornavn mangler i retur fra PDL")
  public void skalKasteNullpointerExceptionDersomFornavnManglerIReturFraPdl() {

    // given
    var fnrOppgittFar =
        "01018512345() -> assertTrue(registrertNavn.getFornavn().equals(returnertNavn.getFornavn())";
    stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
    var registrertNavn =
        NavnDto.builder().mellomnavn("Parafin").etternavn("Olsen").build();
    List<HentPersonSubQuery> subQueries = List.of(new HentPersonNavn(registrertNavn));
    pdlApiStub.runPdlApiHentPersonStub(subQueries);

    // when, then
    assertThrows(NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));

  }

  @Test
  @DisplayName("Skal kaste nullpointerexception dersom fornavn mangler i retur fra PDL")
  public void skalKasteNullpointerExceptionDersomEtternavnManglerIReturFraPdl() {

    // given
    var fnrOppgittFar =
        "01018512345() -> assertTrue(registrertNavn.getFornavn().equals(returnertNavn.getFornavn())";
    stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
    var registrertNavn =
        NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").build();
    List<HentPersonSubQuery> subQueries = List.of(new HentPersonNavn(registrertNavn));
    pdlApiStub.runPdlApiHentPersonStub(subQueries);

    // when, then
    assertThrows(NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));

  }

  @Test
  @DisplayName("Skal kaste PdlApiException hvis person ikke eksisterer")
  void skalKastePdlApiExceptionHvisPersonIkkeEksisterer() {

    // given
    var fnrMor = "111222240280";
    stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
    pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();

    // when, then
    assertThrows(PdlApiException.class, () -> pdlApiConsumer.henteKjoenn(fnrMor));
  }
}
