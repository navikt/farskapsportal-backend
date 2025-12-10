package no.nav.farskapsportal.backend.apps.api.consumer.pdl;

import static no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK;
import static no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.config.FarskapsportalApiConfig;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonBostedsadresse;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonDoedsfall;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonFoedested;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonFoedselsdato;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonFolkeregisteridentifikator;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonForelderBarnRelasjon;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonSivilstand;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonSubResponse;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.HentPersonVergeEllerFremtidsfullmakt;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.pdl.DoedsfallDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.FolkeregisteridentifikatorDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonRolle;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonRolle.Sivilstandtype;
import no.nav.farskapsportal.backend.libs.dto.pdl.KjoennType;
import no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.VergeEllerFullmektigDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.VergemaalEllerFremtidsfullmaktDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.BostedsadresseDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.VegadresseDto;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.RestTemplateFellesConfig;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.exception.RessursIkkeFunnetException;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@EnableMockOAuth2Server
@ActiveProfiles(PROFILE_TEST)
@DirtiesContext
@DisplayName("PdlApiConsumer")
@AutoConfigureWireMock(port = 0)
@SpringBootTest(
    classes = FarskapsportalApiApplicationLocal.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {FarskapsportalApiConfig.class, RestTemplateFellesConfig.class})
public class PdlApiConsumerTest {

  private static final Forelder MOR = henteForelder(Forelderrolle.MOR);

  private @Autowired PdlApiConsumer pdlApiConsumer;
  private @Autowired PdlApiStub pdlApiStub;
  private @Autowired CacheManager cacheManager;
  private @MockBean OAuth2AccessTokenService oAuth2AccessTokenService;
  private @MockBean OAuth2AccessTokenResponse oAuth2AccessTokenResponse;
  private @MockBean GcpStorageManager gcpStorageManager;

  private void mockAccessToken() {
    when(oAuth2AccessTokenService.getAccessToken(any()))
        .thenReturn(new OAuth2AccessTokenResponse("123", 1, 1, Collections.emptyMap()));
  }

  @Nested
  @DisplayName("Hente kjønn")
  class Kjoenn {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("kjoenn").clear();
    }

    @Test
    public void skalHenteKjoennMedSyntetiskIdent() {

      // given
      var fnrMor = "03827297045";
      mockAccessToken();
      var kjoennshistorikk = new LinkedHashMap<LocalDateTime, KjoennType>();
      kjoennshistorikk.put(LocalDateTime.now().minusYears(30), KjoennType.KVINNE);

      List<HentPersonSubResponse> subResponses = List.of(new HentPersonKjoenn(kjoennshistorikk));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var kjoenn = pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor);

      // then
      assertEquals(KjoennType.KVINNE, kjoenn.getKjoenn());
    }

    @Test
    @DisplayName("Skal hente kjønn hvis person eksisterer")
    public void skalHenteKjoennHvisPersonEksisterer() {

      // given
      var fnrMor = "111222240280";
      mockAccessToken();
      var kjoennshistorikk = new LinkedHashMap<LocalDateTime, KjoennType>();
      kjoennshistorikk.put(LocalDateTime.now().minusYears(30), KjoennType.KVINNE);

      List<HentPersonSubResponse> subResponses = List.of(new HentPersonKjoenn(kjoennshistorikk));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var kjoenn = pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor);

      // then
      assertEquals(KjoennType.KVINNE, kjoenn.getKjoenn());
    }

    @Test
    @DisplayName("Skal kaste PersonIkkeFunnetException hvis person ikke eksisterer")
    void skalKastePersonIkkeFunnetExceptionHvisPersonIkkeEksisterer() {

      // given
      var fnrMor = "111222240280";
      pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();
      mockAccessToken();

      // when, then
      assertThrows(
          RessursIkkeFunnetException.class, () -> pdlApiConsumer.henteKjoennUtenHistorikk(fnrMor));
    }

    @Test
    @DisplayName("Skal kaste PdlApiErrorException ved valideringfeil hos PDL")
    void skalKastePdlApiErrorExceptionVedValideringsfeilHosPdl() {

      // given
      var fnrMor = "111222240280";
      mockAccessToken();
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
      mockAccessToken();

      var kjoennshistorikk = new LinkedHashMap<LocalDateTime, KjoennType>();
      kjoennshistorikk.put(LocalDateTime.now().minusYears(30), KjoennType.KVINNE);
      kjoennshistorikk.put(LocalDateTime.now().minusYears(4), KjoennType.MANN);

      List<HentPersonSubResponse> subResponses = List.of(new HentPersonKjoenn(kjoennshistorikk));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var historikk = pdlApiConsumer.henteKjoennMedHistorikk(fnrMor);

      // then
      assertAll(
          () -> assertThat(historikk.size()).isEqualTo(2),
          () ->
              assertThat(
                      historikk.stream()
                          .filter(k -> k.getKjoenn().equals(KjoennType.MANN))
                          .findFirst()
                          .get()
                          .getMetadata()
                          .getHistorisk())
                  .isFalse(),
          () ->
              assertThat(
                      historikk.stream()
                          .filter(k -> k.getKjoenn().equals(KjoennType.KVINNE))
                          .findFirst()
                          .get()
                          .getMetadata()
                          .getHistorisk())
                  .isTrue());
    }

    @Test
    @DisplayName(
        "Skal kaste PersonIkkeFunnetException hvis person med kjønnshistorikk ikke eksisterer")
    void skalKastePersonIkkeFunnetExceptionHvisPersonMedKjoennshistorikkIkkeEksisterer() {

      // given
      var fnrMor = "111222240289";
      mockAccessToken();
      pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();

      // when, then
      assertThrows(
          RessursIkkeFunnetException.class, () -> pdlApiConsumer.henteKjoennMedHistorikk(fnrMor));
    }
  }

  @Nested
  @DisplayName("Hente navn")
  class Navn {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("navn").clear();
    }

    @Test
    @DisplayName("Skal returnere navn til person dersom fødselsnummer eksisterer")
    public void skalReturnereNavnTilPersonDersomFnrEksisterer() {

      // given
      var fnrOppgittFar = "01018512345";
      var registrertNavn =
          NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").etternavn("Olsen").build();
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonNavn(registrertNavn));
      mockAccessToken();
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

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
      var registrertNavn = NavnDto.builder().mellomnavn("Parafin").etternavn("Olsen").build();
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonNavn(registrertNavn));
      mockAccessToken();
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when, then
      assertThrows(
          NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));
    }

    @Test
    @DisplayName("Skal kaste nullpointerexception dersom fornavn mangler i retur fra PDL")
    public void skalKasteNullpointerExceptionDersomEtternavnManglerIReturFraPdl() {

      // given
      var fnrOppgittFar = "01018512345";
      var registrertNavn = NavnDto.builder().fornavn("Pelle").mellomnavn("Parafin").build();
      mockAccessToken();
      List<HentPersonSubResponse> subResponses = List.of(new HentPersonNavn(registrertNavn));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when, then
      assertThrows(
          NullPointerException.class, () -> pdlApiConsumer.hentNavnTilPerson(fnrOppgittFar));
    }
  }

  @Nested
  @DisplayName("Hente folkeregisteridentifikator")
  class Folkeregisteridentifikator {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("folkeregisteridentifikator").clear();
    }

    @Test
    void skalHenteFolkeregisteridentifikatorForMor() {

      // given
      List<HentPersonSubResponse> subResponses =
          List.of(
              new HentPersonFolkeregisteridentifikator(
                  FolkeregisteridentifikatorDto.builder()
                      .identifikasjonsnummer(MOR.getFoedselsnummer())
                      .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                      .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                      .build()));
      mockAccessToken();
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var folkeregisteridentifikatorDto =
          pdlApiConsumer.henteFolkeregisteridentifikator(MOR.getFoedselsnummer());

      // then
      assertAll(
          () ->
              assertThat(folkeregisteridentifikatorDto.getStatus())
                  .isEqualTo(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK),
          () ->
              assertThat(folkeregisteridentifikatorDto.getType())
                  .isEqualTo(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR));
    }
  }

  @Nested
  @DisplayName("Hente fødselsdato")
  class Foedselsdato {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("foedselsdato").clear();
    }

    @Test
    @DisplayName("Skal hente fødselsdato for eksisterende person")
    void skalHenteFoedselsdatoForEksisterendePerson() {
      var morsFoedselsdato = LocalDate.of(1993, 4, 3);

      // given
      var fnrMor = "030493240280";
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonFoedselsdato(morsFoedselsdato, false));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var fødselsdatoDto = pdlApiConsumer.henteFoedselsdato(fnrMor);

      // then
      assertEquals(
          morsFoedselsdato,
          fødselsdatoDto.getFoedselsdato(),
          "Mors fødselsdato skal være den samme som den returnerte datoen");
    }
  }

  @Nested
  @DisplayName("Hente fødested")
  class Foedested {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("foedested").clear();
    }

    @Test
    void skalHenteFødestedForPerson() {
      var morsFødselsdato = LocalDate.of(1993, 4, 3);

      // given
      var fnrMor = "030493240280";
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonFoedested(fnrMor, "Tana", false));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var fødestedDto = pdlApiConsumer.henteFoedested(fnrMor);

      // then
      assertEquals(
          "Tana",
          fødestedDto.getFoedested(),
          "Mors fødested skal være den samme som det registrerte fødestedet");
    }
  }

  @Nested
  @DisplayName("Hente forelderBarnRelasjon")
  class ForelderBarnRelasjon {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("forelderBarnReleasjon").clear();
    }

    @Test
    @DisplayName(
        "Skal ikke feile dersom  mor ikke har noen forelder-barn-relasjon før barnet er født")
    void skalIkkeFeileDersomMorIkkeHarForelderBarnRelasjonFoerFoedsel() {

      // given
      var fnrMor = "13108411110";
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonForelderBarnRelasjon(null, "1234"));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var morsForelderBarnRelasjon = pdlApiConsumer.henteForelderBarnRelasjon(fnrMor);

      // then
      assertEquals(
          morsForelderBarnRelasjon.size(), 0, "Mor har ingen forelderBarnRelasjon før fødsel");
    }

    @Test
    @DisplayName("Skal hente forelder-barn-relasjon for far")
    void skalHenteForelderBarnRelasjonForFar() {

      // given
      var fnrFar = "13108411111";
      var fnrBarn = "01112009091";
      mockAccessToken();
      var forelderBarnRelasjonDto =
          ForelderBarnRelasjonDto.builder()
              .relatertPersonsIdent(fnrBarn)
              .relatertPersonsRolle(ForelderBarnRelasjonRolle.BARN)
              .minRolleForPerson(ForelderBarnRelasjonRolle.FAR)
              .build();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonForelderBarnRelasjon(forelderBarnRelasjonDto, "1234"));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var farsForelderBarnRelasjoner = pdlApiConsumer.henteForelderBarnRelasjon(fnrFar);

      // then
      assertAll(
          () ->
              assertEquals(
                  fnrBarn,
                  farsForelderBarnRelasjoner.stream()
                      .map(ForelderBarnRelasjonDto::getRelatertPersonsIdent)
                      .findAny()
                      .get()),
          () ->
              assertEquals(
                  forelderBarnRelasjonDto.getMinRolleForPerson(),
                  farsForelderBarnRelasjoner.stream()
                      .map(ForelderBarnRelasjonDto::getMinRolleForPerson)
                      .findAny()
                      .get()),
          () ->
              assertEquals(
                  forelderBarnRelasjonDto.getRelatertPersonsRolle(),
                  farsForelderBarnRelasjoner.stream()
                      .map(ForelderBarnRelasjonDto::getRelatertPersonsRolle)
                      .findAny()
                      .get()));
    }
  }

  @Nested
  @DisplayName("Hente sivilstand")
  class Sivilstand {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("sivilstand").clear();
    }

    @Test
    @DisplayName("Skal hente sivilstand for far")
    void skalHenteSivilstandForFar() {

      // given
      var fnrFar = "13108411111";
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonSivilstand(Sivilstandtype.UGIFT));
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
      var fnr = "1310841511";
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonSivilstand(Sivilstandtype.GIFT));
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
      var fnr = "13108411311";
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonSivilstand(Sivilstandtype.UOPPGITT));
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

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("bostedsadresse").clear();
    }

    @Test
    void skalHenteBostedsadresseForMorBosattINorge() {

      // given
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(
              new HentPersonBostedsadresse(
                  BostedsadresseDto.builder()
                      .vegadresse(
                          VegadresseDto.builder()
                              .adressenavn("Stortingsgaten")
                              .husnummer("5")
                              .husbokstav("B")
                              .postnummer("0202")
                              .build())
                      .build()));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var bostedsadresseDto = pdlApiConsumer.henteBostedsadresse(MOR.getFoedselsnummer());

      // then
      assertAll(
          () ->
              assertThat(bostedsadresseDto.getVegadresse().getAdressenavn())
                  .isEqualTo("Stortingsgaten"),
          () -> assertThat(bostedsadresseDto.getVegadresse().getHusnummer()).isEqualTo("5"),
          () -> assertThat(bostedsadresseDto.getVegadresse().getPostnummer()).isEqualTo("0202"));
    }
  }

  @Nested
  @DisplayName("Hente doedsfall")
  class Doedsfall {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("doedsfall").clear();
    }

    @Test
    void skalHenteInformasjonOmDoedsfallForLevendePerson() {

      // given
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonDoedsfall(DoedsfallDto.builder().doedsdato(null).build()));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var doedsfallDto = pdlApiConsumer.henteDoedsfall(FAR.getFoedselsnummer());

      // then
      assertThat(doedsfallDto).isNull();
    }

    @Test
    void skalHenteInformasjonOmDoedsfallForDoedPerson() {

      // given
      mockAccessToken();
      var doedsdato = LocalDate.now().minusWeeks(5);
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonDoedsfall(DoedsfallDto.builder().doedsdato(doedsdato).build()));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var doedsfallDto = pdlApiConsumer.henteDoedsfall(FAR.getFoedselsnummer());

      // then
      assertAll(
          () -> assertThat(doedsfallDto).isNotNull(),
          () -> assertThat(doedsfallDto.getDoedsdato()).isEqualTo(doedsdato));
    }
  }

  @Nested
  @DisplayName("Hente vergeEllerFremtidsfullmakt")
  class VergeEllerFremtidsfullmakt {

    @BeforeEach
    void clearCache() {
      cacheManager.getCache("verge").clear();
    }

    @Test
    @DisplayName("Skal hente vergeEllerFremtidsfullmakt for person med verge")
    void skalHenteVergeEllerFremtidsfullmaktForEksisterendePerson() {

      // given
      mockAccessToken();
      var vergemaalEllerFremtidsfullmaktDto =
          VergemaalEllerFremtidsfullmaktDto.builder()
              .type("voksen")
              .embete("Fylkesmannen i Innlandet")
              .vergeEllerFullmektig(
                  VergeEllerFullmektigDto.builder()
                      .omfang("personligeOgOekonomiskeInteresser")
                      .build())
              .build();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonVergeEllerFremtidsfullmakt(vergemaalEllerFremtidsfullmaktDto));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var vergemaalEllerFremtidsfullmaktDtos =
          pdlApiConsumer.henteVergeEllerFremtidsfullmakt(MOR.getFoedselsnummer());

      // then
      assertAll(
          () -> assertThat(vergemaalEllerFremtidsfullmaktDtos.size()).isEqualTo(1),
          () -> assertThat(vergemaalEllerFremtidsfullmaktDtos.get(0).getType()).isEqualTo("voksen"),
          () ->
              assertThat(vergemaalEllerFremtidsfullmaktDtos.get(0).getEmbete())
                  .isEqualTo("Fylkesmannen i Innlandet"),
          () ->
              assertThat(
                      vergemaalEllerFremtidsfullmaktDtos
                          .get(0)
                          .getVergeEllerFullmektig()
                          .getOmfang())
                  .isEqualTo("personligeOgOekonomiskeInteresser"));
    }

    @Test
    @DisplayName("Skal returnere tom vergeEllerFremtidsfullmakt for person uten verge")
    void skalReturnereTomVergeEllerFremtidsfullmaktForPersonUtenVerge() {

      // given
      mockAccessToken();
      List<HentPersonSubResponse> subResponses =
          List.of(new HentPersonVergeEllerFremtidsfullmakt(null));
      pdlApiStub.runPdlApiHentPersonStub(subResponses);

      // when
      var vergemaalEllerFremtidsfullmaktDtos =
          pdlApiConsumer.henteVergeEllerFremtidsfullmakt(MOR.getFoedselsnummer());

      // then
      assertThat(vergemaalEllerFremtidsfullmaktDtos.size()).isEqualTo(0);
    }
  }
}
