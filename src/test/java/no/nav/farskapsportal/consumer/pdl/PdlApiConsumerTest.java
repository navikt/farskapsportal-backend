package no.nav.farskapsportal.consumer.pdl;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Sivilstandtype;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonerDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFamilierelasjoner;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFoedsel;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSivilstand;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSubQuery;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PdlApiConsumer")
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = {FarskapsportalApplicationLocal.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 8096)
public class PdlApiConsumerTest {

  @Autowired
  private PdlApiConsumer pdlApiConsumer;

  @Autowired
  private PdlApiStub pdlApiStub;

  @Autowired
  private StsStub stsStub;

  @Nested
  @DisplayName("Hente kjønn")
  class Kjoenn {

    @Test
    @DisplayName("Skal hente kjønn hvis person eksisterer")
    public void skalHenteKjoennHvisPersonEksisterer() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonKjoenn(KjoennType.KVINNE));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var kjoenn = pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor);

      // then
      Assertions.assertEquals(KjoennType.KVINNE, kjoenn.getKjoenn());
    }

    @Test
    @DisplayName("Skal kaste PersonIkkeFunnetException hvis person ikke eksisterer")
    void skalKastePersonIkkeFunnetExceptionHvisPersonIkkeEksisterer() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();

      // when, then
      assertThrows(RessursIkkeFunnetException.class, () -> pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor));
    }

    @Test
    @DisplayName("Skal kaste PdlApiErrorException ved valideringfeil hos PDL")
    void skalKastePdlApiErrorExceptionVedValideringsfeilHosPdl() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      pdlApiStub.runPdlApiHentPersonValideringsfeil();

      // when, then
      assertThrows(PdlApiErrorException.class, () -> pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor));
    }

    @Test
    @DisplayName("Skal hente kjønnshistorikk for eksisterende person")
    void skalHenteKjoennshistorikkForEksisterendePerson() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");

      var map = Stream
          .of(new Object[][]{{KjoennType.KVINNE, LocalDateTime.now().minusYears(30)}, {KjoennType.MANN, LocalDateTime.now().minusYears(4)}})
          .collect(Collectors.toMap(data -> (KjoennType) data[0], data -> (LocalDateTime) data[1]));

      var sortedMap = new TreeMap<KjoennType, LocalDateTime>(map);
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonKjoenn(sortedMap));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var historikk = pdlApiConsumer.henteKjoennMedHistorikk(fnrMor);

      // then
      assertAll(() -> assertEquals(historikk.size(), 2, "Historikken skal inneholde to elementer"),
          () -> assertTrue(historikk.stream().filter(k -> k.getKjoenn().equals(KjoennType.MANN)).findFirst().get().getMetadata().getHistorisk(),
              "Personen har mann som historisk kjønn"), () -> Assertions
              .assertFalse(historikk.stream().filter(k -> k.getKjoenn().equals(KjoennType.KVINNE)).findFirst().get().getMetadata().getHistorisk(),
                  "Personen har kvinne som gjeldende kjønn"));
    }

    @Test
    @DisplayName("Skal kaste PersonIkkeFunnetException hvis person med kjønnshistorikk ikke eksisterer")
    void skalKastePersonIkkeFunnetExceptionHvisPersonMedKjoennshistorikkIkkeEksisterer() {

      // given
      var fnrMor = "111222240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();

      // when, then
      assertThrows(RessursIkkeFunnetException.class, () -> pdlApiConsumer.henteKjoennMedHistorikk(fnrMor));
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
      var registrertNavn = NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").etternavn("Olsen").build();
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var navnDto = pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar);

      // then
      assertAll(() -> assertEquals(registrertNavn.getFornavn(), navnDto != null ? navnDto.getFornavn() : null),
          () -> assertEquals(registrertNavn.getMellomnavn(), navnDto != null ? navnDto.getMellomnavn() : null),
          () -> assertEquals(registrertNavn.getEtternavn(), navnDto != null ? navnDto.getEtternavn() : null));
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
      assertThrows(NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));
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
      assertThrows(NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));
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
      assertEquals(morsFoedselsdato, returnertFoedselsdato, "Mors fødselsdato skal være den samme som den returnerte datoen");
    }
  }

  @Nested
  @DisplayName("Hente familierelasjoner")
  class FamilieRelasjoner {

    @Test
    @DisplayName("Skal ikke feile dersom  mor ikke har noen familierelasjoner før barnet er født")
    void skalIkkeFeileDersomMorIkkeHarFamilierelasjonerFoerFoedsel() {

      // given
      var fnrMor = "13108411110";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonFamilierelasjoner(null, "1234"));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var farsFamilierelasjoner = pdlApiConsumer.henteFamilierelasjoner(fnrMor);

      // then
      assertEquals(farsFamilierelasjoner.size(), 0, "Mor har ingen familierelasjoner før fødsel");
    }

    @Test
    @DisplayName("Skal hente familierelasjoner for far")
    void skalHenteFamilieRelasjonerForFar() {

      // given
      var fnrFar = "13108411111";
      var fnrBarn = "01112009091";
      var familierelasjonerDto = FamilierelasjonerDto.builder().relatertPersonsIdent(fnrBarn).relatertPersonsRolle(FamilierelasjonRolle.BARN)
          .minRolleForPerson(FamilierelasjonRolle.FAR).build();
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonFamilierelasjoner(familierelasjonerDto, "1234"));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var farsFamilierelasjoner = pdlApiConsumer.henteFamilierelasjoner(fnrFar);

      // then
      assertAll(() -> assertEquals(fnrBarn, farsFamilierelasjoner.stream().map(FamilierelasjonerDto::getRelatertPersonsIdent).findAny().get()),
          () -> assertEquals(familierelasjonerDto.getMinRolleForPerson(),
              farsFamilierelasjoner.stream().map(FamilierelasjonerDto::getMinRolleForPerson).findAny().get()),
          () -> assertEquals(familierelasjonerDto.getRelatertPersonsRolle(),
              farsFamilierelasjoner.stream().map(FamilierelasjonerDto::getRelatertPersonsRolle).findAny().get()));
    }
  }

  @Nested
  @DisplayName("Hente sivilstand")
  class Sivilstand {

    @Test
    @DisplayName("Skal hente sivilstand for far")
    void skalHenteSivilstandForFar() {

      // given
      var fnrFar = "13108411111";

      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonSivilstand(Sivilstandtype.UGIFT));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var farsSivilstand = pdlApiConsumer.henteSivilstand(fnrFar);

      // then
      assertEquals(Sivilstandtype.UGIFT, farsSivilstand.getType());
    }

    @Test
    @DisplayName("Skal hente sivilstand gift dersom person er gift")
    void skalHenteSivilstandGiftDersomPersonErGift() {
      // given
      var fnr = "13108411111";

      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonSivilstand(Sivilstandtype.GIFT));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var sivilstand = pdlApiConsumer.henteSivilstand(fnr);

      // then
      assertEquals(Sivilstandtype.GIFT, sivilstand.getType());
    }

    @Test
    @DisplayName("Skal hente sivilstand uoppgitt dersom sivilstand ikke er registrert")
    void skalHenteSivilstandUoppgittDersomSivilstandIkkeErRegistrert() {
      // given
      var fnr = "13108411111";

      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubQuery> subQueries = List.of(new HentPersonSivilstand(Sivilstandtype.UOPPGITT));
      pdlApiStub.runPdlApiHentPersonStub(subQueries);

      // when
      var personensSivilstand = pdlApiConsumer.henteSivilstand(fnr);

      // then
      assertEquals(Sivilstandtype.UOPPGITT, personensSivilstand.getType());
    }

  }
}
