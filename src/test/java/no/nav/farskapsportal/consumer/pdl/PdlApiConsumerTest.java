package no.nav.farskapsportal.consumer.pdl;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.FAR;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static no.nav.farskapsportal.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK;
import static no.nav.farskapsportal.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.Sivilstandtype;
import no.nav.farskapsportal.consumer.pdl.api.DoedsfallDto;
import no.nav.farskapsportal.consumer.pdl.api.ForelderBarnRelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.ForelderBarnRelasjonDto;
import no.nav.farskapsportal.consumer.pdl.api.FolkeregisteridentifikatorDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.BostedsadresseDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.VegadresseDto;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonBostedsadresse;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonDoedsfall;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonForelderBarnRelasjon;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFoedsel;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFolkeregisteridentifikator;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSivilstand;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSubResponse;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.RessursIkkeFunnetException;
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

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);

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
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonKjoenn(KjoennType.KVINNE));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

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
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonKjoenn(sortedMap));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

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
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

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

  @Nested
  @DisplayName("Hente folkeregisteridentifikator")
  class Folkeregisteridentifikator {

    @Test
    void skalHenteFolkeregisteridentifikatorForMor() {

      // given
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonFolkeregisteridentifikator(
          FolkeregisteridentifikatorDto.builder().identifikasjonsnummer(MOR.getFoedselsnummer()).type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
              .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build()));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var folkeregisteridentifikatorDto = pdlApiConsumer.henteFolkeregisteridentifikator(MOR.getFoedselsnummer());

      // then
      assertAll(
          () -> assertThat(folkeregisteridentifikatorDto.getStatus()).isEqualTo(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK),
          () -> assertThat(folkeregisteridentifikatorDto.getType()).isEqualTo(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
      );
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
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonFoedsel(morsFoedselsdato, false));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var foedselDto = pdlApiConsumer.henteFoedsel(fnrMor);

      // then
      assertEquals(morsFoedselsdato, foedselDto.getFoedselsdato(), "Mors fødselsdato skal være den samme som den returnerte datoen");
    }

    @Test
    void skalHenteFoedestedForPerson() {
      var morsFoedselsdato = LocalDate.of(1993, 4, 3);

      // given
      var fnrMor = "030493240280";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonFoedsel(morsFoedselsdato, "Tana", false));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var foedselDto = pdlApiConsumer.henteFoedsel(fnrMor);

      // then
      assertEquals("Tana", foedselDto.getFoedested(), "Mors fødselsdato skal være den samme som den returnerte datoen");
    }
  }

  @Nested
  @DisplayName("Hente forelderBarnRelasjon")
  class ForelderBarnRelasjon {

    @Test
    @DisplayName("Skal ikke feile dersom  mor ikke har noen forelder-barn-relasjon før barnet er født")
    void skalIkkeFeileDersomMorIkkeHarForelderBarnRelasjonFoerFoedsel() {

      // given
      var fnrMor = "13108411110";
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonForelderBarnRelasjon(null, "1234"));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var morsForelderBarnRelasjon = pdlApiConsumer.henteForelderBarnRelasjon(fnrMor);

      // then
      assertEquals(morsForelderBarnRelasjon.size(), 0, "Mor har ingen forelderBarnRelasjon før fødsel");
    }

    @Test
    @DisplayName("Skal hente forelder-barn-relasjon for far")
    void skalHenteForelderBarnRelasjonForFar() {

      // given
      var fnrFar = "13108411111";
      var fnrBarn = "01112009091";
      var forelderBarnRelasjonDto = ForelderBarnRelasjonDto.builder().relatertPersonsIdent(fnrBarn).relatertPersonsRolle(ForelderBarnRelasjonRolle.BARN)
          .minRolleForPerson(ForelderBarnRelasjonRolle.FAR).build();
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonForelderBarnRelasjon(forelderBarnRelasjonDto, "1234"));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var farsForelderBarnRelasjoner = pdlApiConsumer.henteForelderBarnRelasjon(fnrFar);

      // then
      assertAll(() -> assertEquals(fnrBarn, farsForelderBarnRelasjoner.stream().map(ForelderBarnRelasjonDto::getRelatertPersonsIdent).findAny().get()),
          () -> assertEquals(forelderBarnRelasjonDto.getMinRolleForPerson(),
              farsForelderBarnRelasjoner.stream().map(ForelderBarnRelasjonDto::getMinRolleForPerson).findAny().get()),
          () -> assertEquals(forelderBarnRelasjonDto.getRelatertPersonsRolle(),
              farsForelderBarnRelasjoner.stream().map(ForelderBarnRelasjonDto::getRelatertPersonsRolle).findAny().get()));
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
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonSivilstand(Sivilstandtype.UGIFT));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

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
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonSivilstand(Sivilstandtype.GIFT));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

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
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonSivilstand(Sivilstandtype.UOPPGITT));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var personensSivilstand = pdlApiConsumer.henteSivilstand(fnr);

      // then
      assertEquals(Sivilstandtype.UOPPGITT, personensSivilstand.getType());
    }
  }

  @Nested
  @DisplayName("Hente bostedsadresse")
  class Bostedsadresse {

    @Test
    void skalHenteBostedsadresseForMorBosattINorge() {

      // given
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonBostedsadresse(BostedsadresseDto.builder()
          .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("5").husbokstav("B").postnummer("0202").build()).build()));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var bostedsadresseDto = pdlApiConsumer.henteBostedsadresse(MOR.getFoedselsnummer());

      // then
      assertAll(
          () -> assertThat(bostedsadresseDto.getVegadresse().getAdressenavn()).isEqualTo("Stortingsgaten"),
          () -> assertThat(bostedsadresseDto.getVegadresse().getHusnummer()).isEqualTo("5"),
          () -> assertThat(bostedsadresseDto.getVegadresse().getPostnummer()).isEqualTo("0202")
      );
    }
  }

  @Nested
  @DisplayName("Hente doedsfall")
  class Doedsfall {

    @Test
    void skalHenteInformasjonOmDoedsfallForLevendePerson() {

      // given
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonDoedsfall(DoedsfallDto.builder().doedsdato(null).build()));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var doedsfallDto = pdlApiConsumer.henteDoedsfall(FAR.getFoedselsnummer());

      // then
      assertThat(doedsfallDto).isNull();

    }

    @Test
    void skalHenteInformasjonOmDoedsfallForDoedPerson() {

      // given
      var doedsdato = LocalDate.now().minusWeeks(5);
      stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonDoedsfall(DoedsfallDto.builder().doedsdato(doedsdato).build()));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var doedsfallDto = pdlApiConsumer.henteDoedsfall(FAR.getFoedselsnummer());

      // then
      assertAll(
          () -> assertThat(doedsfallDto).isNotNull(),
          () -> assertThat(doedsfallDto.getDoedsdato()).isEqualTo(doedsdato)
      );

    }

  }
}
