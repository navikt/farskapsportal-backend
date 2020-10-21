package no.nav.farskapsportal.consumer.pdl;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonerDto;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFamilierelasjoner;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFoedsel;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSubQuery;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
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

  @Nested
  @DisplayName("Hente kjønn")
  class Kjoenn {

    @Test
    @DisplayName("Skal hente kjønn hvis person eksisterer")
    public void skalHenteKjoennHvisPersonEksisterer() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries =
          List.of(new HentPersonKjoenn(no.nav.farskapsportal.api.Kjoenn.KVINNE));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var kjoenn = pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor);

      // then
      Assertions.assertEquals(no.nav.farskapsportal.api.Kjoenn.KVINNE, kjoenn);
    }

    @Test
    @DisplayName("Skal kaste PersonIkkeFunnetException hvis person ikke eksisterer")
    void skalKastePersonIkkeFunnetExceptionHvisPersonIkkeEksisterer() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();

      // when, then
      assertThrows(
          PersonIkkeFunnetException.class, () -> pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor));
    }

    @Test
    @DisplayName("Skal kaste PdlApiErrorException ved valideringfeil hos PDL")
    void skalKastePdlApiErrorExceptionVedValideringsfeilHosPdl() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      pdlApiStub.runPdlApiHentPersonValideringsfeil();

      // when, then
      assertThrows(
          PdlApiErrorException.class, () -> pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor));
    }

    @Test
    @DisplayName("Skal hente kjønnshistorikk for eksisterende person")
    void skalHenteKjoennshistorikkForEksisterendePerson() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries =
          List.of(
              new HentPersonKjoenn(
                  List.of(
                      no.nav.farskapsportal.api.Kjoenn.KVINNE,
                      no.nav.farskapsportal.api.Kjoenn.MANN)));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var historikk = pdlApiConsumer.henteKjoennMedHistorikk(fnrMor);

      // then
      assertAll(
          () -> assertEquals(historikk.size(), 2, "Historikken skal inneholde to elementer"),
          () ->
              Assertions.assertEquals(
                  no.nav.farskapsportal.api.Kjoenn.KVINNE,
                  historikk.stream().findFirst().get(),
                  "Første element i historikken er kvinne"));
    }

    @Test
    @DisplayName(
        "Skal kaste PersonIkkeFunnetException hvis person med kjønnshistorikk ikke eksisterer")
    void skalKastePersonIkkeFunnetExceptionHvisPersonMedKjoennshistorikkIkkeEksisterer() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();

      // when, then
      assertThrows(
          PersonIkkeFunnetException.class, () -> pdlApiConsumer.henteKjoennMedHistorikk(fnrMor));
    }
  }

  @Nested
  @DisplayName("Hente navn")
  class Navn {

    @Test
    @DisplayName("Skal returnere navn til person dersom fødselsnummer eksisterer")
    public void skalReturnereNavnTilPersonDersomFnrEksisterer() {

      // given
      var fnrOppgittFar = "01018512345";
      stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
      var registrertNavn =
          NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").etternavn("Olsen").build();
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var navnDto = pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar);

      // then
      assertAll(
          () ->
              assertEquals(
                  registrertNavn.getFornavn(), navnDto != null ? navnDto.getFornavn() : null),
          () ->
              assertEquals(
                  registrertNavn.getMellomnavn(), navnDto != null ? navnDto.getMellomnavn() : null),
          () ->
              assertEquals(
                  registrertNavn.getEtternavn(), navnDto != null ? navnDto.getEtternavn() : null));
    }

    @Test
    @DisplayName("Skal kaste nullpointerexception dersom fornavn mangler i retur fra PDL")
    public void skalKasteNullpointerExceptionDersomFornavnManglerIReturFraPdl() {

      // given
      var fnrOppgittFar = "01018512345";
      stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
      var registrertNavn = NavnDto.builder().mellomnavn("Parafin").etternavn("Olsen").build();
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when, then
      assertThrows(
          NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));
    }

    @Test
    @DisplayName("Skal kaste nullpointerexception dersom fornavn mangler i retur fra PDL")
    public void skalKasteNullpointerExceptionDersomEtternavnManglerIReturFraPdl() {

      // given
      var fnrOppgittFar = "01018512345";
      stsStub.runSecurityTokenServiceStub("eyQ25gkasgag");
      var registrertNavn = NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").build();
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when, then
      assertThrows(
          NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));
    }
  }

  @Nested
  @DisplayName("Hente fødselsdato")
  class Foedsel {
    @Test
    @DisplayName("Skal hente fødselsdato for eksisterende person")
    void skalHenteFoedselsdatoForEksisterendePerson() {
      var morsFoedselsdato = LocalDate.of(1993, 4, 3);

      // given
      var fnrMor = "030493240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonFoedsel(morsFoedselsdato, false));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var returnertFoedselsdato = pdlApiConsumer.henteFoedselsdato(fnrMor);

      // then
      assertEquals(
          morsFoedselsdato,
          returnertFoedselsdato,
          "Mors fødselsdato skal være den samme som den returnerte datoen");
    }
  }

  @Nested
  @DisplayName("Hente familierelasjoner")
  class FamilieRelasjoner {

    @Test
    @DisplayName("Skal hente familierelasjoner for far")
    void skalHenteFamilieRelasjonerForFar() {

      // given
      var fnrFar = "13108411111";
      var fnrBarn = "01112009091";
      var familierelasjonerDto = FamilierelasjonerDto.builder().relatertPersonsIdent(fnrBarn).relatertPersonsRolle(
          FamilierelasjonRolle.BARN).minRolleForPerson(FamilierelasjonRolle.FAR).build();
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonFamilierelasjoner(familierelasjonerDto, "1234"));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var farsFamilierelasjoner = pdlApiConsumer.henteFamilierelasjoner(fnrFar);

      // then
      assertAll(
          () -> assertEquals(fnrBarn, farsFamilierelasjoner.stream().map(FamilierelasjonerDto::getRelatertPersonsIdent).findAny().get()),
          () -> assertEquals(familierelasjonerDto.getMinRolleForPerson(), farsFamilierelasjoner.stream().map(FamilierelasjonerDto::getMinRolleForPerson).findAny().get()),
          () -> assertEquals(familierelasjonerDto.getRelatertPersonsRolle(), farsFamilierelasjoner.stream().map(FamilierelasjonerDto::getRelatertPersonsRolle).findAny().get())
      );
    }
  }
}
