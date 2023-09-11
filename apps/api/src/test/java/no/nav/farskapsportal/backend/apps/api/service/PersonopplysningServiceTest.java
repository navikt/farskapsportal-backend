package no.nav.farskapsportal.backend.apps.api.service;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.KODE_LAND_NORGE;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.NAVN_FAR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.pdl.DoedsfallDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.EndringDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.EndringDto.Type;
import no.nav.farskapsportal.backend.libs.dto.pdl.FoedselDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonRolle;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonRolle.Sivilstandtype;
import no.nav.farskapsportal.backend.libs.dto.pdl.KjoennDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.KjoennType;
import no.nav.farskapsportal.backend.libs.dto.pdl.MetadataDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.SivilstandDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.VergeEllerFullmektigDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.VergemaalEllerFremtidsfullmaktDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.BostedsadresseDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.UtenlandskAdresseDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.VegadresseDto;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig;
<<<<<<< HEAD
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageWrapper;
=======
>>>>>>> main
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.KontrollereNavnFarException;
import no.nav.farskapsportal.backend.libs.felles.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@DisplayName("PersonopplysningService")
@ActiveProfiles(FarskapsportalFellesConfig.PROFILE_TEST)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApiApplicationLocal.class)
public class PersonopplysningServiceTest {

  private static final Forelder MOR = TestUtils.henteForelder(Forelderrolle.MOR);
  private static final Forelder FAR = TestUtils.henteForelder(Forelderrolle.FAR);
  private static final LocalDate FOEDSELSDATO_NYFOEDT = LocalDate.now().minusMonths(1);
  private static final Barn NYDFOEDT_BARN =
      TestUtils.henteBarnMedFnr(FOEDSELSDATO_NYFOEDT, "00000");

<<<<<<< HEAD
  private @MockBean PdlApiConsumer pdlApiConsumerMock;
  private @MockBean GcpStorageWrapper gcpStorageWrapper;

  private @Autowired PersonopplysningService personopplysningService;
=======
  @MockBean private PdlApiConsumer pdlApiConsumerMock;

  @Autowired private PersonopplysningService personopplysningService;
>>>>>>> main

  private KjoennDto henteKjoenn(KjoennType typeKjoenn) {
    return henteKjoenn(typeKjoenn, MetadataDto.builder().historisk(false).build());
  }

  private KjoennDto henteKjoenn(
      KjoennType typeKjoenn, LocalDateTime registreringstidspunkt, boolean historisk) {
    var metadata =
        MetadataDto.builder()
            .historisk(historisk)
            .endringer(
                List.of(
                    EndringDto.builder()
                        .type(Type.OPPRETT)
                        .registrert(registreringstidspunkt)
                        .build()))
            .build();
    return henteKjoenn(typeKjoenn, metadata);
  }

  private KjoennDto henteKjoenn(KjoennType typeKjoenn, MetadataDto metadata) {
    return KjoennDto.builder().kjoenn(typeKjoenn).metadata(metadata).build();
  }

  @Nested
  @DisplayName("Tester harNorskBostedsadress")
  class HarNorskBostedsadresse {

    @Test
    void skalReturnereSannForPersonBosattINorge() {

      // given
      var bostedsadresseDto =
          BostedsadresseDto.builder()
              .vegadresse(
                  VegadresseDto.builder()
                      .adressenavn("Hovedveien")
                      .husnummer("80")
                      .postnummer("3030")
                      .build())
              .build();

      when(pdlApiConsumerMock.henteBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(bostedsadresseDto);

      // when
      var adressestreng = personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer());

      // then
      assertThat(adressestreng).isTrue();
    }

    @Test
    void skalReturnereUsannForPersonUtenNorskBostedsadresse() {

      // given
      var bostedsadresseDto =
          BostedsadresseDto.builder()
              .utenlandskAdresse(
                  UtenlandskAdresseDto.builder().adressenavnNummer("123 Parkway Avenue.").build())
              .build();

      when(pdlApiConsumerMock.henteBostedsadresse(FAR.getFoedselsnummer()))
          .thenReturn(bostedsadresseDto);

      // when
      var adressestreng = personopplysningService.harNorskBostedsadresse(FAR.getFoedselsnummer());

      // then
      assertThat(adressestreng).isFalse();
    }
  }

  @Nested
  @DisplayName("Tester henteGjeldendeKjoenn")
  class HenteGjeldendeKjoenn {

    @Test
    @DisplayName(
        "Gjeldene kjønn skal være kvinne dersom dette er personens registrerte kjønn i PDL")
    void gjeldendeKjoennSkalVaereKvinneDersomPersonenHarKvinneSomRegistrertKjoennIPdl() {

      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var gjeldendeKjoenn = henteKjoenn(KjoennType.KVINNE);

      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(gjeldendeKjoenn);

      // when
      var gjeldendeKjoennReturnert = personopplysningService.henteGjeldendeKjoenn(foedselsnummer);

      // then
      assertEquals(
          KjoennType.KVINNE,
          gjeldendeKjoennReturnert.getKjoenn(),
          "Gjeldende kjønn skal være kvinne");
    }
  }

  @Nested
  @DisplayName("Tester henteFoedeland")
  class HenteFoedeland {

    @Test
    void skalHenteFoedelandForNyfoedt() {

      var personnummerNyfoedt = "12345";
      var foedselsdatoNyfoedt = LocalDate.now().minusMonths(2).minusDays(13);
      var foedselsnummerNyfoedt =
          foedselsdatoNyfoedt.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerNyfoedt;

      when(pdlApiConsumerMock.henteFoedsel(foedselsnummerNyfoedt))
          .thenReturn(
              FoedselDto.builder()
                  .foedselsdato(foedselsdatoNyfoedt)
                  .foedeland(KODE_LAND_NORGE)
                  .build());

      // when
      var foedeland = personopplysningService.henteFoedeland(foedselsnummerNyfoedt);

      // then
      assertThat(foedeland).isEqualTo(KODE_LAND_NORGE);
    }
  }

  @Nested
  @DisplayName("Tester henteFoedselsdato")
  class HenteFoedselsdato {

    @DisplayName("Skal hente fødselsdato for person i PDL")
    @Test
    void skalHenteFoedselsdatoForPersonIPdl() {

      // given
      var personnummerMor = "13130";
      var foedselsdatoMor = LocalDate.now().minusYears(25).minusMonths(2).minusDays(13);
      var fnrMor = foedselsdatoMor.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerMor;

      when(pdlApiConsumerMock.henteFoedsel(fnrMor))
          .thenReturn(FoedselDto.builder().foedselsdato(foedselsdatoMor).build());

      // when
      var returnertFoedselsdato = personopplysningService.henteFoedselsdato(fnrMor);

      // then
      assertEquals(
          foedselsdatoMor,
          returnertFoedselsdato,
          "Returnert fødselsdato skal være lik mors fødselsdato");
    }
  }

  @Nested
  @DisplayName("Tester henteNavn")
  class HenteNavn {

    @Test
    @DisplayName("Skal hente navn til person som eksisterer i folkeregisteret")
    void skalHenteNavnTilEksisterendePerson() {

      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var navnDto = NavnDto.builder().fornavn("Ronald").etternavn("McDonald").build();

      when(pdlApiConsumerMock.hentNavnTilPerson(foedselsnummer)).thenReturn(navnDto);

      // when
      var returnertNavnDto = personopplysningService.henteNavn(foedselsnummer);

      // then
      assertAll(
          () ->
              assertEquals(
                  navnDto.getFornavn(),
                  returnertNavnDto.getFornavn(),
                  "Skal returnere riktig fornavn"),
          () ->
              assertEquals(
                  navnDto.getEtternavn(),
                  returnertNavnDto.getEtternavn(),
                  "Skal returnere riktig etternavn"));
    }
  }

  @Nested
  @DisplayName(" Tester navnekontroll")
  class Navnekontroll {

    @Test
    void skalIkkeKasteExceptionDersomOppgittNavnStemmerMedRegister() {

      // given
      var registrertNavn = NAVN_FAR;
      registrertNavn.setMellomnavn("Danger");
      var oppgittNavn = NAVN_FAR.getFornavn() + " Danger " + NAVN_FAR.getEtternavn();

      // when, then
      assertDoesNotThrow(
          () ->
              personopplysningService.navnekontroll(oppgittNavn, registrertNavn.sammensattNavn()));
    }

    @Test
    void skalKastekontrollereNavnFarExceptionDersomOppgittNavnIkkeStemmerMedRegister() {

      // given
      var farsRegistrerteNavn = NAVN_FAR;
      farsRegistrerteNavn.setMellomnavn("Danger");
      var farsNavn = NAVN_FAR.getFornavn() + " Dangerous " + NAVN_FAR.getEtternavn();

      // when
      var kontrollereNavnFarException =
          assertThrows(
              KontrollereNavnFarException.class,
              () ->
                  personopplysningService.navnekontroll(
                      farsNavn, farsRegistrerteNavn.sammensattNavn()));

      // then
      assertThat(kontrollereNavnFarException.getFeilkode())
          .isEqualTo(Feilkode.NAVN_STEMMER_IKKE_MED_REGISTER);
    }

    @Test
    void skalLikestilleÈMedE() {
      // given
      var farsRegistrerteNavn =
          no.nav.farskapsportal.backend.libs.dto.NavnDto.builder()
              .fornavn("Donald Andre")
              .mellomnavn("Dangerous")
              .etternavn(NAVN_FAR.getEtternavn())
              .build();
      var farsNavn = "Donald André" + " Dangerous " + NAVN_FAR.getEtternavn();

      // when, then
      assertDoesNotThrow(
          () ->
              personopplysningService.navnekontroll(
                  farsNavn, farsRegistrerteNavn.sammensattNavn()));
    }

    @Test
    void skalHaandtereNorskeBokstaver() {
      // given
      var farsRegistrerteNavn =
          no.nav.farskapsportal.backend.libs.dto.NavnDto.builder()
              .fornavn("Dånald Øndræ")
              .mellomnavn("Dængerås")
              .etternavn(NAVN_FAR.getEtternavn())
              .build();
      var farsNavn = "Dånald Øndræ" + " Dængerås " + NAVN_FAR.getEtternavn();

      // when, then
      assertDoesNotThrow(
          () ->
              personopplysningService.navnekontroll(
                  farsNavn, farsRegistrerteNavn.sammensattNavn()));
    }

    @Test
    void skalHaandtereSvenskeTilstander() {
      // given
      var farsRegistrerteNavn =
          no.nav.farskapsportal.backend.libs.dto.NavnDto.builder()
              .fornavn("Donald Andre")
              .mellomnavn("Dengeraas")
              .etternavn(NAVN_FAR.getEtternavn())
              .build();
      var oppgittNavnPaaFar = "Dönald André" + " Dengeraas " + NAVN_FAR.getEtternavn();

      var t = UUID.randomUUID().toString();

      var sn = farsRegistrerteNavn.sammensattNavn();
      var test = sn.replace(oppgittNavnPaaFar, "");

      // when, then
      assertDoesNotThrow(
          () ->
              personopplysningService.navnekontroll(
                  oppgittNavnPaaFar, farsRegistrerteNavn.sammensattNavn()));
    }
  }

  @Nested
  @DisplayName(" Tester erOver18Aar")
  class ErOver18Aar {

    @Test
    void skalReturnereSannForPersonOver18Aar() {

      // given
      when(pdlApiConsumerMock.henteFoedsel(FAR.getFoedselsnummer()))
          .thenReturn(FoedselDto.builder().foedselsdato(FOEDSELSDATO_FAR).build());

      // when
      var farErMyndig = personopplysningService.erOver18Aar(FAR.getFoedselsnummer());

      // then
      assertThat(farErMyndig).isTrue();
    }

    @Test
    void skalReturnereUsannForPersonUnder18Aar() {

      // given
      when(pdlApiConsumerMock.henteFoedsel(NYDFOEDT_BARN.getFoedselsnummer()))
          .thenReturn(FoedselDto.builder().foedselsdato(FOEDSELSDATO_NYFOEDT).build());

      // when
      var erMyndig = personopplysningService.erOver18Aar(NYDFOEDT_BARN.getFoedselsnummer());

      // then
      assertThat(erMyndig).isFalse();
    }
  }

  @Nested
  @DisplayName("Tester erDoed")
  class ErDoed {

    @Test
    void skalReturnereSannForPersonMedRegistrertDoedsdato() {

      // given
      when(pdlApiConsumerMock.henteDoedsfall(FAR.getFoedselsnummer()))
          .thenReturn(DoedsfallDto.builder().doedsdato(LocalDate.now().minusMonths(1)).build());

      // when
      var farErDoed = personopplysningService.erDoed(FAR.getFoedselsnummer());

      // then
      assertThat(farErDoed).isTrue();
    }

    @Test
    void skalReturnereUsannForPersonSomIkkeHarRegistrertDoedsdato() {

      // given
      when(pdlApiConsumerMock.henteDoedsfall(FAR.getFoedselsnummer())).thenReturn(null);

      // when
      var farErDoed = personopplysningService.erDoed(FAR.getFoedselsnummer());

      // then
      assertThat(farErDoed).isFalse();
    }
  }

  @Nested
  @DisplayName("Tester harVerge")
  class HarVerge {

    @Test
    void skalReturnereSannDersomOmfangErNull() {

      // given
      when(pdlApiConsumerMock.henteVergeEllerFremtidsfullmakt(FAR.getFoedselsnummer()))
          .thenReturn(
              List.of(
                  VergemaalEllerFremtidsfullmaktDto.builder()
                      .vergeEllerFullmektig(VergeEllerFullmektigDto.builder().omfang(null).build())
                      .build()));

      // when
      var farHarVerge = personopplysningService.harVerge(FAR.getFoedselsnummer());

      // then
      assertThat(farHarVerge).isTrue();
    }

    @Test
    void skalReturnereSannDersomPersonHarVergeMedOmfangPersonligeInteresser() {

      // given
      when(pdlApiConsumerMock.henteVergeEllerFremtidsfullmakt(FAR.getFoedselsnummer()))
          .thenReturn(
              List.of(
                  VergemaalEllerFremtidsfullmaktDto.builder()
                      .vergeEllerFullmektig(
                          VergeEllerFullmektigDto.builder().omfang("personligeInteresser").build())
                      .build()));

      // when
      var farHarVerge = personopplysningService.harVerge(FAR.getFoedselsnummer());

      // then
      assertThat(farHarVerge).isTrue();
    }

    @Test
    void skalReturnereSannDersomPersonHarVergeMedOmfangPersonligeOgOekonomiskeInteresser() {

      // given
      when(pdlApiConsumerMock.henteVergeEllerFremtidsfullmakt(FAR.getFoedselsnummer()))
          .thenReturn(
              List.of(
                  VergemaalEllerFremtidsfullmaktDto.builder()
                      .vergeEllerFullmektig(
                          VergeEllerFullmektigDto.builder()
                              .omfang("personligeOgOekonomiskeInteresser")
                              .build())
                      .build()));

      // when
      var farHarVerge = personopplysningService.harVerge(FAR.getFoedselsnummer());

      // then
      assertThat(farHarVerge).isTrue();
    }

    @Test
    void
        skalReturnereUsannDersomPersonHarVergeMedOmfangUtlendingssakerPersonligeOgOekonomiskeInteresser() {

      // given
      when(pdlApiConsumerMock.henteVergeEllerFremtidsfullmakt(FAR.getFoedselsnummer()))
          .thenReturn(
              List.of(
                  VergemaalEllerFremtidsfullmaktDto.builder()
                      .vergeEllerFullmektig(
                          VergeEllerFullmektigDto.builder()
                              .omfang("utlendingssakerPersonligeOgOekonomiskeInteresser")
                              .build())
                      .build()));

      // when
      var farHarVerge = personopplysningService.harVerge(FAR.getFoedselsnummer());

      // then
      assertThat(farHarVerge).isFalse();
    }

    @Test
    void skalReturnereUsannDersomPersonIkkeErRegistrertMedVerge() {

      // given
      when(pdlApiConsumerMock.henteVergeEllerFremtidsfullmakt(FAR.getFoedselsnummer()))
          .thenReturn(new ArrayList<>());

      // when
      var farHarVerge = personopplysningService.harVerge(FAR.getFoedselsnummer());

      // then
      assertThat(farHarVerge).isFalse();
    }
  }

  @Nested
  @DisplayName("Tester riktigNavnOgKjoennOppgitt")
  class RiktigNavnOgKjoennOppgitt {

    @Test
    @DisplayName("Skal hente forelderrolle for eksisterende person")
    void skalHenteForelderrolleForEksisterendePerson() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var kjoenn = henteKjoenn(KjoennType.KVINNE);

      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(kjoenn);
      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer)).thenReturn(List.of(kjoenn));

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      Assertions.assertEquals(Forelderrolle.MOR, forelderrolle);
    }

    @Test
    @DisplayName("Skal kaste PersonIkkeFunnetException dersom informasjon om person mangler")
    void skalKastePersonIkkeFunnetExceptionDersomInformasjonOmPersonMangler() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;

      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer))
          .thenThrow(RessursIkkeFunnetException.class);

      // when, then
      assertThrows(
          RessursIkkeFunnetException.class,
          () -> personopplysningService.bestemmeForelderrolle(foedselsnummer));
    }
  }

  @Nested
  @DisplayName("Tester henteSivilstand")
  class HenteSivilstand {

    @Test
    @DisplayName("Skal hente sivilstand til forelder")
    void skalHenteSivilstandTilForelder() {

      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      NavnDto.builder().fornavn("Ronaldina").etternavn("McDonald").build();

      when(pdlApiConsumerMock.henteSivilstand(foedselsnummer))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());

      // when
      var returnertSivilstand = personopplysningService.henteSivilstand(foedselsnummer);

      // then
      assertEquals(
          Sivilstandtype.UGIFT, returnertSivilstand.getType(), "Skal returnere riktig sivilstand");
    }
  }

  @Nested
  @DisplayName("Tester henteNyligFoedteBarnUtenRegistrertFar")
  class HenteNyligFoedteBarnUtenRegistrertFar {

    @DisplayName(
        "Skal hente nyfødt barn uten registrert far dersom relasjon mellom mor og barn eksisterer")
    @Test
    void skalHenteNyfoedtBarnUtenRegistrertFarDersomRelasjonMellomMorOgBarnEksisterer() {

      // given
      var personnummerTvilling1 = "12345";
      var personnummerTvilling2 = "12344";
      var foedselsdatoTvillinger = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrMor =
          foedselsdatoTvillinger
                  .plusYears(29)
                  .plusMonths(2)
                  .plusDays(13)
                  .format(DateTimeFormatter.ofPattern("ddMMyy"))
              + "24680";
      var fnrNyfoedtTvilling1 =
          foedselsdatoTvillinger.format(DateTimeFormatter.ofPattern("ddMMyy"))
              + personnummerTvilling1;
      var fnrNyfoedtTvilling2 =
          foedselsdatoTvillinger.format(DateTimeFormatter.ofPattern("ddMMyy"))
              + personnummerTvilling2;

      var tvilling1 =
          ForelderBarnRelasjonDto.builder()
              .relatertPersonsIdent(fnrNyfoedtTvilling1)
              .minRolleForPerson(ForelderBarnRelasjonRolle.MOR)
              .relatertPersonsRolle(ForelderBarnRelasjonRolle.BARN)
              .build();

      var tvilling2 =
          ForelderBarnRelasjonDto.builder()
              .relatertPersonsIdent(fnrNyfoedtTvilling2)
              .minRolleForPerson(ForelderBarnRelasjonRolle.MOR)
              .relatertPersonsRolle(ForelderBarnRelasjonRolle.BARN)
              .build();

      when(pdlApiConsumerMock.henteForelderBarnRelasjon(fnrMor))
          .thenReturn(List.of(tvilling1, tvilling2));
      when(pdlApiConsumerMock.henteFoedsel(anyString()))
          .thenReturn(
              FoedselDto.builder()
                  .foedselsdato(foedselsdatoTvillinger)
                  .foedeland(KODE_LAND_NORGE)
                  .build());

      // when
      var nyligFoedteBarnUtenRegistrertFar =
          personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

      // then
      assertAll(
          () ->
              assertTrue(
                  nyligFoedteBarnUtenRegistrertFar.contains(fnrNyfoedtTvilling1),
                  "Nyfødt tvilling 1 skal være med i lista"),
          () ->
              assertTrue(
                  nyligFoedteBarnUtenRegistrertFar.contains(fnrNyfoedtTvilling2),
                  "Nyfødt tvilling 2 skal være med i lista "));
    }

    @Test
    @DisplayName("Skal ikke inkludere spedbarn med registrert far")
    void skalIkkeInkludereSpedbarnMedRegistrertFar() {

      // given
      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrSpedbarn =
          foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "00011";
      var fnrMor =
          foedselsdatoSpedbarn
                  .plusYears(29)
                  .plusMonths(2)
                  .plusDays(13)
                  .format(DateTimeFormatter.ofPattern("ddMMyy"))
              + "24680";
      var fnrFar =
          foedselsdatoSpedbarn
                  .plusYears(31)
                  .plusMonths(7)
                  .plusDays(5)
                  .format(DateTimeFormatter.ofPattern("ddMMyy"))
              + "24680";

      var morsRelasjonTilSpedbarn =
          ForelderBarnRelasjonDto.builder()
              .relatertPersonsIdent(fnrSpedbarn)
              .minRolleForPerson(ForelderBarnRelasjonRolle.MOR)
              .relatertPersonsRolle(ForelderBarnRelasjonRolle.BARN)
              .build();

      var spedbarnsRelasjonTilFar =
          ForelderBarnRelasjonDto.builder()
              .relatertPersonsIdent(fnrFar)
              .minRolleForPerson(ForelderBarnRelasjonRolle.BARN)
              .relatertPersonsRolle(ForelderBarnRelasjonRolle.FAR)
              .build();

      when(pdlApiConsumerMock.henteForelderBarnRelasjon(fnrMor))
          .thenReturn(List.of(morsRelasjonTilSpedbarn));
      when(pdlApiConsumerMock.henteForelderBarnRelasjon(fnrSpedbarn))
          .thenReturn(List.of(spedbarnsRelasjonTilFar));
      when(pdlApiConsumerMock.henteFoedsel(fnrSpedbarn))
          .thenReturn(FoedselDto.builder().foedselsdato(foedselsdatoSpedbarn).build());

      // when
      var nyligFoedteBarnUtenRegistrertFar =
          personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

      // then
      assertEquals(
          0,
          nyligFoedteBarnUtenRegistrertFar.size(),
          "Spedbarn med registrert far skal ikke returneres");
    }

    @Test
    void skalIkkeInkludereBarnFoedtUtenforNorge() {

      // given
      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrSpedbarn =
          foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "00011";
      var fnrMor =
          foedselsdatoSpedbarn
                  .plusYears(29)
                  .plusMonths(2)
                  .plusDays(13)
                  .format(DateTimeFormatter.ofPattern("ddMMyy"))
              + "24680";

      var morsRelasjonTilSpedbarn =
          ForelderBarnRelasjonDto.builder()
              .relatertPersonsIdent(fnrSpedbarn)
              .minRolleForPerson(ForelderBarnRelasjonRolle.MOR)
              .relatertPersonsRolle(ForelderBarnRelasjonRolle.BARN)
              .build();

      when(pdlApiConsumerMock.henteForelderBarnRelasjon(fnrMor))
          .thenReturn(List.of(morsRelasjonTilSpedbarn));
      when(pdlApiConsumerMock.henteFoedsel(fnrSpedbarn))
          .thenReturn(
              FoedselDto.builder().foedselsdato(foedselsdatoSpedbarn).foedeland("UGANDA").build());

      // when
      var nyligFoedteBarnUtenRegistrertFar =
          personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

      // then
      assertEquals(
          0,
          nyligFoedteBarnUtenRegistrertFar.size(),
          "Spedbarn med registrert far skal ikke returneres");
    }

    @Test
    void skalIkkeInkludereBarnMedUkjentFoedested() {

      // given
      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrSpedbarn =
          foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "00011";
      var fnrMor =
          foedselsdatoSpedbarn
                  .plusYears(29)
                  .plusMonths(2)
                  .plusDays(13)
                  .format(DateTimeFormatter.ofPattern("ddMMyy"))
              + "24680";

      var morsRelasjonTilSpedbarn =
          ForelderBarnRelasjonDto.builder()
              .relatertPersonsIdent(fnrSpedbarn)
              .minRolleForPerson(ForelderBarnRelasjonRolle.MOR)
              .relatertPersonsRolle(ForelderBarnRelasjonRolle.BARN)
              .build();

      when(pdlApiConsumerMock.henteForelderBarnRelasjon(fnrMor))
          .thenReturn(List.of(morsRelasjonTilSpedbarn));
      when(pdlApiConsumerMock.henteFoedsel(fnrSpedbarn))
          .thenReturn(
              FoedselDto.builder().foedselsdato(foedselsdatoSpedbarn).foedeland(null).build());

      // when
      var nyligFoedteBarnUtenRegistrertFar =
          personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

      // then
      assertEquals(
          0,
          nyligFoedteBarnUtenRegistrertFar.size(),
          "Spedbarn med registrert far skal ikke returneres");
    }

    @Test
    @DisplayName("Skal ikke gi feilmelding dersom antall barn uten registrert far er 0")
    void skalIkkeGiFeilmeldingDersomAntallBarnUtenRegistrertFarErNull() {

      // given
      var foedselsdatoMor = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrMor =
          foedselsdatoMor
                  .plusYears(29)
                  .plusMonths(2)
                  .plusDays(13)
                  .format(DateTimeFormatter.ofPattern("ddMMyy"))
              + "24680";

      when(pdlApiConsumerMock.henteForelderBarnRelasjon(fnrMor)).thenReturn(new ArrayList<>());

      // when
      var nyligFoedteBarnUtenRegistrertFar =
          personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

      // then
      assertEquals(
          0,
          nyligFoedteBarnUtenRegistrertFar.size(),
          "Mor har ingen registrerte barn som mangler far");
    }
  }

  @Nested
  @DisplayName("Tester bestemmeForelderrolle")
  class BestemmeForelderrolle {

    @DisplayName("Person født kvinne med gjeldende kjønn mann skal gi forelderrolle MOR_ELLER_FAR ")
    @Test
    void foedeKjoennKvinneOgGjeldendeKjoennMannSkalGiForelderrolleMorEllerFar() {

      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn =
          henteKjoenn(KjoennType.KVINNE, LocalDateTime.of(foedselsdato, LocalTime.now()), true);
      var endretKjoenn = henteKjoenn(KjoennType.MANN, LocalDateTime.now().minusYears(10), false);

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer))
          .thenReturn(List.of(originaltKjoenn, endretKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(endretKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      Assertions.assertEquals(
          Forelderrolle.MOR_ELLER_FAR,
          forelderrolle,
          "En person som er født kvinne skal kunne opptre som både mor og far i løsningen, og ha foreldrerolle MOR_ELLER_FAR");
    }

    @DisplayName("Person med føde- og gjeldende kjønn kvinne skal ha forelderrolle MOR")
    @Test
    void foedeOgGjeldendeKjoennKvinneSkalGiForelderrolleMor() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn =
          henteKjoenn(KjoennType.KVINNE, LocalDateTime.of(foedselsdato, LocalTime.now()), true);

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer))
          .thenReturn(List.of(originaltKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(originaltKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      Assertions.assertEquals(
          Forelderrolle.MOR,
          forelderrolle,
          "En person som er født kvinne og ikke endrer kjønn, skal ha foreldrerolle MOR");
    }

    @DisplayName("Person født mann med gjeldende kjønn kvinne skal ha forelderrolle MEDMOR")
    @Test
    void foedeKjoennMannOgGjeldendeKjoennKvinneSkalGiForelderrolleMedmor() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn = henteKjoenn(KjoennType.MANN, LocalDateTime.now().minusYears(35), true);

      var endretKjoenn = henteKjoenn(KjoennType.KVINNE, LocalDateTime.now().minusYears(10), false);

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer))
          .thenReturn(List.of(originaltKjoenn, endretKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(endretKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      Assertions.assertEquals(
          Forelderrolle.MEDMOR,
          forelderrolle,
          "En person som er født mann, men endrer kjønn til kvinne skal ha foreldrerolle MEDMOR");
    }

    @Test
    void
        foedeKjoennMannOgGjeldendeKjoennKvinneSkalGiForelderrolleMedmorManglerGyldighetstidspunkt() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn =
          henteKjoenn(KjoennType.MANN, LocalDateTime.of(foedselsdato, LocalTime.now()), true);
      var endretKjoenn = henteKjoenn(KjoennType.KVINNE, LocalDateTime.now().minusYears(10), false);

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer))
          .thenReturn(List.of(originaltKjoenn, endretKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(endretKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      Assertions.assertEquals(
          Forelderrolle.MEDMOR,
          forelderrolle,
          "En person som er født mann, men endrer kjønn til kvinne skal ha foreldrerolle MEDMOR");
    }

    @DisplayName("Person med mann som både fødekjønn og gjeldende kjønn skal ha forelderrolle FAR")
    @Test
    void foedeKjoennOgGjeldendeKjoennMannSkalGiForelderrolleFar() {

      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn = henteKjoenn(KjoennType.MANN);

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer))
          .thenReturn(List.of(originaltKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(originaltKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      Assertions.assertEquals(
          Forelderrolle.FAR,
          forelderrolle,
          "En person som er født mann, og som også har mann som gjeldende kjønn skal ha foreldrerolle FAR.");
    }

    @DisplayName("Person med gjeldende kjønn UKJENT skal ha forelderrolle UKJENT")
    @Test
    void gjeldendeKjoennUkjentSkalGiForelderrolleUkjent() {
      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var gjeldendeKjoenn = henteKjoenn(KjoennType.UKJENT);

      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(gjeldendeKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      Assertions.assertEquals(
          Forelderrolle.UKJENT,
          forelderrolle,
          "En person med UKJENT som gjeldende kjønn, skal ha foreldrerolle UKJENT.");
    }
  }
}
