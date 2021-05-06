package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static no.nav.farskapsportal.service.FarskapsportalService.KODE_LAND_NORGE;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.Sivilstandtype;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.api.DoedsfallDto;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonerDto;
import no.nav.farskapsportal.consumer.pdl.api.FoedselDto;
import no.nav.farskapsportal.consumer.pdl.api.FolkeregistermetadataDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.api.SivilstandDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.BostedsadresseDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.UtenlandskAdresseDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.VegadresseDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.exception.ValideringException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PersonopplysningService")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class PersonopplysningServiceTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto NYDFOEDT_BARN = henteBarnMedFnr(LocalDate.now().minusMonths(1), "00000");

  @MockBean
  private PdlApiConsumer pdlApiConsumerMock;

  @Autowired
  private PersonopplysningService personopplysningService;

  private KjoennDto henteKjoennMedGyldighetstidspunkt(KjoennType typeKjoenn, LocalDate datoForGyldighet) {
    return KjoennDto.builder().kjoenn(typeKjoenn).folkeregistermetadata(FolkeregistermetadataDto.builder()
        .gyldighetstidspunkt(LocalDateTime.ofEpochSecond(datoForGyldighet.toEpochSecond(LocalTime.MIN, ZoneOffset.MIN), 0, ZoneOffset.MIN)).build())
        .build();
  }

  @Nested
  @DisplayName("Tester harNorskBostedsadress")
  class HarNorskBostedsadresse {

    @Test
    void skalReturnereSannForPersonBosattINorge() {

      // given
      var bostedsadresseDto = BostedsadresseDto.builder()
          .vegadresse(VegadresseDto.builder().adressenavn("Hovedveien").husnummer("80").postnummer("3030").build()).build();

      when(pdlApiConsumerMock.henteBostedsadresse(MOR.getFoedselsnummer())).thenReturn(bostedsadresseDto);

      // when
      var adressestreng = personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer());

      // then
      assertThat(adressestreng).isTrue();
    }

    @Test
    void skalReturnereUsannForPersonUtenNorskBostedsadresse() {

      // given
      var bostedsadresseDto = BostedsadresseDto.builder().utenlandskAdresse(
          UtenlandskAdresseDto.builder().adressenavnNummer("123 Parkway Avenue.").build()).build();

      when(pdlApiConsumerMock.henteBostedsadresse(FAR.getFoedselsnummer())).thenReturn(bostedsadresseDto);

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
    @DisplayName("Gjeldene kjønn skal være kvinne dersom dette er personens registrerte kjønn i PDL")
    void gjeldendeKjoennSkalVaereKvinneDersomPersonenHarKvinneSomRegistrertKjoennIPdl() {

      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var gjeldendeKjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.KVINNE, foedselsdato);

      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(gjeldendeKjoenn);

      // when
      var gjeldendeKjoennReturnert = personopplysningService.henteGjeldendeKjoenn(foedselsnummer);

      // then
      assertEquals(KjoennType.KVINNE, gjeldendeKjoennReturnert.getKjoenn(), "Gjeldende kjønn skal være kvinne");
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

      when(pdlApiConsumerMock.henteFoedsel(fnrMor)).thenReturn(FoedselDto.builder().foedselsdato(foedselsdatoMor).build());

      // when
      var returnertFoedselsdato = personopplysningService.henteFoedselsdato(fnrMor);

      // then
      assertEquals(foedselsdatoMor, returnertFoedselsdato, "Returnert fødselsdato skal være lik mors fødselsdato");
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
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var navnDto = NavnDto.builder().fornavn("Ronald").etternavn("McDonald").build();

      when(pdlApiConsumerMock.hentNavnTilPerson(foedselsnummer)).thenReturn(navnDto);

      // when
      var returnertNavnDto = personopplysningService.henteNavn(foedselsnummer);

      // then
      assertAll(() -> assertEquals(navnDto.getFornavn(), returnertNavnDto.getFornavn(), "Skal returnere riktig fornavn"),
          () -> assertEquals(navnDto.getEtternavn(), returnertNavnDto.getEtternavn(), "Skal returnere riktig etternavn"));
    }
  }

  @Nested
  @DisplayName(" Tester navnekontroll")
  class Navnekontroll {

    @Test
    void skalIkkeKasteExceptionDersomOppgittNavnStemmerMedRegister() {

      // given
      var farsRegistrerteNavn = NavnDto.builder().fornavn(FAR.getFornavn()).mellomnavn("Danger").etternavn(FAR.getEtternavn()).build();
      var farsNavn = FAR.getFornavn() + " Danger " + FAR.getEtternavn();

      // when, then
      assertDoesNotThrow(() -> personopplysningService.navnekontroll(farsNavn, farsRegistrerteNavn));
    }

    @Test
    void skalKasteValideringExceptionDersomOppgittNavnIkkeStemmerMedRegister() {

      // given
      var farsRegistrerteNavn = NavnDto.builder().fornavn(FAR.getFornavn()).mellomnavn("Danger").etternavn(FAR.getEtternavn()).build();
      var farsNavn = FAR.getFornavn() + " Dangerous " + FAR.getEtternavn();

      // when, then
      assertThrows(ValideringException.class, () -> personopplysningService.navnekontroll(farsNavn, farsRegistrerteNavn));
    }

  }

  @Nested
  @DisplayName(" Tester erMyndig")
  class ErMyndig {

    @Test
    void skalReturnereSannForMyndigPerson() {

      // given
      when(pdlApiConsumerMock.henteFoedsel(FAR.getFoedselsnummer())).thenReturn(FoedselDto.builder().foedselsdato(FAR.getFoedselsdato()).build());

      // when
      var farErMyndig = personopplysningService.erMyndig(FAR.getFoedselsnummer());

      // then
      assertThat(farErMyndig).isTrue();
    }

    @Test
    void skalReturnereUsannForUmyndigPerson() {

      // given
      when(pdlApiConsumerMock.henteFoedsel(NYDFOEDT_BARN.getFoedselsnummer())).thenReturn(FoedselDto.builder().foedselsdato(NYDFOEDT_BARN.getFoedselsdato()).build());

      // when
      var erMyndig = personopplysningService.erMyndig(NYDFOEDT_BARN.getFoedselsnummer());

      // then
      assertThat(erMyndig).isFalse();

    }

  }

  @Nested
  @DisplayName(" Tester erDoed")
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
  @DisplayName("Tester riktigNavnOgKjoennOppgitt")
  class RiktigNavnOgKjoennOppgitt {

    @Test
    @DisplayName("Skal hente forelderrolle for eksisterende person")
    void skalHenteForelderrolleForEksisterendePerson() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var kjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.KVINNE, foedselsdato);

      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(kjoenn);
      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer)).thenReturn(List.of(kjoenn));

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      assertEquals(Forelderrolle.MOR, forelderrolle);
    }

    @Test
    @DisplayName("Skal kaste PersonIkkeFunnetException dersom informasjon om person mangler")
    void skalKastePersonIkkeFunnetExceptionDersomInformasjonOmPersonMangler() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;

      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenThrow(RessursIkkeFunnetException.class);

      // when, then
      assertThrows(RessursIkkeFunnetException.class, () -> personopplysningService.bestemmeForelderrolle(foedselsnummer));
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
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      NavnDto.builder().fornavn("Ronaldina").etternavn("McDonald").build();

      when(pdlApiConsumerMock.henteSivilstand(foedselsnummer)).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());

      // when
      var returnertSivilstand = personopplysningService.henteSivilstand(foedselsnummer);

      // then
      assertEquals(Sivilstandtype.UGIFT, returnertSivilstand.getType(), "Skal returnere riktig sivilstand");
    }
  }

  @Nested
  @DisplayName("Tester henteNyligFoedteBarnUtenRegistrertFar")
  class HenteNyligFoedteBarnUtenRegistrertFar {

    @DisplayName("Skal hente nyfødt barn uten registrert far dersom relasjon mellom mor og barn eksisterer")
    @Test
    void skalHenteNyfoedtBarnUtenRegistrertFarDersomRelasjonMellomMorOgBarnEksisterer() {

      // given
      var personnummerTvilling1 = "12345";
      var personnummerTvilling2 = "12344";
      var foedselsdatoTvillinger = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrMor = foedselsdatoTvillinger.plusYears(29).plusMonths(2).plusDays(13).format(DateTimeFormatter.ofPattern("ddMMyy")) + "24680";
      var fnrNyfoedtTvilling1 = foedselsdatoTvillinger.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerTvilling1;
      var fnrNyfoedtTvilling2 = foedselsdatoTvillinger.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerTvilling2;

      var tvilling1 = FamilierelasjonerDto.builder().relatertPersonsIdent(fnrNyfoedtTvilling1).minRolleForPerson(FamilierelasjonRolle.MOR)
          .relatertPersonsRolle(FamilierelasjonRolle.BARN).build();

      var tvilling2 = FamilierelasjonerDto.builder().relatertPersonsIdent(fnrNyfoedtTvilling2).minRolleForPerson(FamilierelasjonRolle.MOR)
          .relatertPersonsRolle(FamilierelasjonRolle.BARN).build();

      when(pdlApiConsumerMock.henteFamilierelasjoner(fnrMor)).thenReturn(List.of(tvilling1, tvilling2));
<<<<<<< HEAD
      when(pdlApiConsumerMock.henteFoedsel(anyString())).thenReturn(FoedselDto.builder().foedselsdato(foedselsdatoTvillinger).foedeland(KODE_LAND_NORGE).build());
=======
      when(pdlApiConsumerMock.henteFoedsel(anyString())).thenReturn(FoedselDto.builder().foedselsdato(foedselsdatoTvillinger).build());
>>>>>>> main

      // when
      var nyligFoedteBarnUtenRegistrertFar = personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

      // then
      assertAll(() -> assertTrue(nyligFoedteBarnUtenRegistrertFar.contains(fnrNyfoedtTvilling1), "Nyfødt tvilling 1 skal være med i lista"),
          () -> assertTrue(nyligFoedteBarnUtenRegistrertFar.contains(fnrNyfoedtTvilling2), "Nyfødt tvilling 2 skal være med i lista "));
    }

    @Test
    @DisplayName("Skal ikke inkludere spedbarn med registrert far")
    void skalIkkeInkludereSpedbarnMedRegistrertFar() {

      // given
      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrSpedbarn = foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "00011";
      var fnrMor = foedselsdatoSpedbarn.plusYears(29).plusMonths(2).plusDays(13).format(DateTimeFormatter.ofPattern("ddMMyy")) + "24680";
      var fnrFar = foedselsdatoSpedbarn.plusYears(31).plusMonths(7).plusDays(5).format(DateTimeFormatter.ofPattern("ddMMyy")) + "24680";

      var morsRelasjonTilSpedbarn = FamilierelasjonerDto.builder().relatertPersonsIdent(fnrSpedbarn).minRolleForPerson(FamilierelasjonRolle.MOR)
          .relatertPersonsRolle(FamilierelasjonRolle.BARN).build();

      var spedbarnsRelasjonTilFar = FamilierelasjonerDto.builder().relatertPersonsIdent(fnrFar).minRolleForPerson(FamilierelasjonRolle.BARN)
          .relatertPersonsRolle(FamilierelasjonRolle.FAR).build();

      when(pdlApiConsumerMock.henteFamilierelasjoner(fnrMor)).thenReturn(List.of(morsRelasjonTilSpedbarn));
      when(pdlApiConsumerMock.henteFamilierelasjoner(fnrSpedbarn)).thenReturn(List.of(spedbarnsRelasjonTilFar));
      when(pdlApiConsumerMock.henteFoedsel(fnrSpedbarn)).thenReturn(FoedselDto.builder().foedselsdato(foedselsdatoSpedbarn).build());

      // when
      var nyligFoedteBarnUtenRegistrertFar = personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

      // then
      assertEquals(0, nyligFoedteBarnUtenRegistrertFar.size(), "Spedbarn med registrert far skal ikke returneres");
    }

    @Test
    @DisplayName("Skal ikke gi feilmelding dersom antall barn uten registrert far er 0")
    void skalIkkeGiFeilmeldingDersomAntallBarnUtenRegistrertFarErNull() {

      // given
      var foedselsdatoMor = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrMor = foedselsdatoMor.plusYears(29).plusMonths(2).plusDays(13).format(DateTimeFormatter.ofPattern("ddMMyy")) + "24680";

      when(pdlApiConsumerMock.henteFamilierelasjoner(fnrMor)).thenReturn(new ArrayList<>());

      // when
      var nyligFoedteBarnUtenRegistrertFar = personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(fnrMor);

      // then
      assertEquals(0, nyligFoedteBarnUtenRegistrertFar.size(), "Mor har ingen registrerte barn som mangler far");
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
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.KVINNE, foedselsdato);
      var endretKjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.MANN, LocalDate.now().minusYears(10));

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer)).thenReturn(List.of(originaltKjoenn, endretKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(endretKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      assertEquals(Forelderrolle.MOR_ELLER_FAR, forelderrolle,
          "En person som er født kvinne skal kunne opptre som både mor og far i løsningen, og ha foreldrerolle MOR_ELLER_FAR");
    }

    @DisplayName("Person med føde- og gjeldende kjønn kvinne skal ha forelderrolle MOR")
    @Test
    void foedeOgGjeldendeKjoennKvinneSkalGiForelderrolleMor() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.KVINNE, foedselsdato);

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer)).thenReturn(List.of(originaltKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(originaltKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      assertEquals(Forelderrolle.MOR, forelderrolle, "En person som er født kvinne og ikke endrer kjønn, skal ha foreldrerolle MOR");
    }

    @DisplayName("Person født mann med gjeldende kjønn kvinne skal ha forelderrolle MEDMOR")
    @Test
    void foedeKjoennMannOgGjeldendeKjoennKvinneSkalGiForelderrolleMedmor() {

      // given
      var personnummer = "12344";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.MANN, foedselsdato);
      var endretKjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.KVINNE, LocalDate.now().minusYears(10));

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer)).thenReturn(List.of(originaltKjoenn, endretKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(endretKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      assertEquals(Forelderrolle.MEDMOR, forelderrolle, "En person som er født mann, men endrer kjønn til kvinne skal ha foreldrerolle MEDMOR");
    }

    @DisplayName("Person med mann som både fødekjønn og gjeldende kjønn skal ha forelderrolle FAR")
    @Test
    void foedeKjoennOgGjeldendeKjoennMannSkalGiForelderrolleFar() {

      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var originaltKjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.MANN, foedselsdato);

      when(pdlApiConsumerMock.henteKjoennMedHistorikk(foedselsnummer)).thenReturn(List.of(originaltKjoenn));
      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(originaltKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      assertEquals(Forelderrolle.FAR, forelderrolle,
          "En person som er født mann, og som også har mann som gjeldende kjønn skal ha foreldrerolle FAR.");
    }

    @DisplayName("Person med gjeldende kjønn UKJENT skal ha forelderrolle UKJENT")
    @Test
    void gjeldendeKjoennUkjentSkalGiForelderrolleUkjent() {
      // given
      var personnummer = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
      var gjeldendeKjoenn = henteKjoennMedGyldighetstidspunkt(KjoennType.UKJENT, foedselsdato);

      when(pdlApiConsumerMock.henteKjoennUtenHistorikk(foedselsnummer)).thenReturn(gjeldendeKjoenn);

      // when
      var forelderrolle = personopplysningService.bestemmeForelderrolle(foedselsnummer);

      // then
      assertEquals(Forelderrolle.UKJENT, forelderrolle, "En person med UKJENT som gjeldende kjønn, skal ha foreldrerolle UKJENT.");
    }
  }
}
