package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.consumer.pdl.api.KjoennTypeDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.RedirectUrlDto;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.DokumentDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.RedirectUrlDao;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PersistenceServiceTest")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class PersistenceServiceTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto BARN = henteBarn(5);
  private static final FarskapserklaeringDto FARSKAPSERKLAERING =
      henteFarskapserklaering(MOR, FAR, BARN);
  @Autowired private PersistenceService persistenceService;
  @Autowired private FarskapserklaeringDao farskapserklaeringDao;
  @Autowired private BarnDao barnDao;
  @Autowired private ForelderDao forelderDao;
  @Autowired private DokumentDao dokumentDao;
  @Autowired private RedirectUrlDao redirectUrlDao;

  private static FarskapserklaeringDto henteFarskapserklaering(
      ForelderDto mor, ForelderDto far, BarnDto barn) {

    var defaultUrl = henteDefaultUrl();

    var redirectUrlMor = RedirectUrlDto.builder().redirectUrl(defaultUrl).signerer(mor).build();
    var redirectUrlFar = RedirectUrlDto.builder().redirectUrl(defaultUrl).signerer(far).build();

    var dokument =
        DokumentDto.builder()
            .dokumentnavn("farskapserklaering.pdf")
            .padesUrl(defaultUrl)
            .redirectUrlMor(redirectUrlMor)
            .redirectUrlFar(redirectUrlFar)
            .build();

    return FarskapserklaeringDto.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }

  private static URI henteDefaultUrl() {
    try {
      return new URI("");
    } catch (URISyntaxException uriSyntaxException) {
      throw new RuntimeException("Feil syntaks i test URI");
    }
  }

  private static ForelderDto henteForelder(Forelderrolle forelderrolle) {
    if (Forelderrolle.MOR.equals(forelderrolle)) {
      var personnummerMor = "12340";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);

      return ForelderDto.builder()
          .foedselsnummer(
              foedselsdato.plusYears(4).format(DateTimeFormatter.ofPattern("ddMMyy"))
                  + personnummerMor)
          .fornavn("Ronaldina")
          .etternavn("McDonald")
          .forelderRolle(Forelderrolle.MOR)
          .build();
    } else {
      var personnummerFar = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);

      return ForelderDto.builder()
          .foedselsnummer(
              foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar)
          .fornavn("Ronald")
          .etternavn("McDonald")
          .forelderRolle(Forelderrolle.FAR)
          .build();
    }
  }

  private static BarnDto henteBarn(int antallMndTilTermindato) {
    var termindato = LocalDate.now().plusMonths(antallMndTilTermindato);
    return BarnDto.builder().termindato(termindato).build();
  }

  @Nested
  @DisplayName("Lagre")
  @DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
  @AutoConfigureTestDatabase(replace = Replace.ANY)
  class Lagre {

    @Test
    @DisplayName("Lagre barn")
    void lagreBarn() {
      persistenceService.lagreBarn(BARN);
      var barnReturnert = barnDao.findById(1).get();

      assertEquals(BARN.getTermindato(), barnReturnert.getTermindato());
    }

    @Test
    @DisplayName("Lagre forelder")
    void lagreForelder() {

      // given, when
      var lagretMor = persistenceService.lagreForelder(MOR);

      var forelder = forelderDao.findById(lagretMor.getId()).get();

      // then
      assertEquals(MOR.getFoedselsnummer(), forelder.getFoedselsnummer());
    }

    @Test
    @DisplayName("Lagre redirectUrl")
    void lagreRedirectUrl() throws URISyntaxException {

      // given
      var redirectUrlDto =
          RedirectUrlDto.builder().redirectUrl(new URI("123")).signerer(MOR).build();

      // when
      var lagretRedirectUrl = persistenceService.lagreRedirectUrl(redirectUrlDto);

      var retur = redirectUrlDao.findById(lagretRedirectUrl.getId()).get();

      // then
      assertEquals(
          redirectUrlDto.getSignerer().getFoedselsnummer(),
          retur.getSignerer().getFoedselsnummer());
    }

    @Test
    @DisplayName("Lagre dokument")
    void lagreDokument() throws URISyntaxException {

      // given
      var redirectUrlMor = RedirectUrlDto.builder().redirectUrl(new URI("")).signerer(MOR).build();
      var redirectUrlFar = RedirectUrlDto.builder().redirectUrl(new URI("")).signerer(FAR).build();

      var dokument =
          DokumentDto.builder()
              .dokumentnavn("farskapserklaring.pdf")
              .padesUrl(new URI(""))
              .redirectUrlMor(redirectUrlMor)
              .redirectUrlFar(redirectUrlFar)
              .build();

      // when
      var lagretDokument = persistenceService.lagreDokument(dokument);

      var hentetDokument = dokumentDao.findById(lagretDokument.getId()).get();
      assertEquals(
          dokument.getRedirectUrlFar().getSignerer().getFoedselsnummer(),
          hentetDokument.getRedirectUrlFar().getSignerer().getFoedselsnummer());
    }

    @Test
    @DisplayName("Lagre farskapserklæring")
    void lagreFarskapserklaering() {

      // given, when
      var lagretFarskapserklaering = persistenceService.lagreFarskapserklaering(FARSKAPSERKLAERING);

      var hentetFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId()).get();

      // then
      assertEquals(
          lagretFarskapserklaering,
          hentetFarskapserklaering,
          "Farskapserklæringen som ble lagret er lik den som ble hentet");
    }
  }

  @Nested
  @DisplayName("Hente")
  @TestInstance(Lifecycle.PER_CLASS)
  class Hente {

    Farskapserklaering lagretFarskapserklaering;

    @BeforeAll
    void setupTestdata() {
      lagretFarskapserklaering = persistenceService.lagreFarskapserklaering(FARSKAPSERKLAERING);
    }

    @Test
    @DisplayName("Skal hente farskapserklæring i forbindelse med mors redirect fra signeringsløsningen")
    void skalHenteFarskapserklaeringEtterRedirectForMor() {

      // given
      var farskapserklaeringUtenPadesUrl = FARSKAPSERKLAERING;
      farskapserklaeringUtenPadesUrl.getDokument().setPadesUrl(null);
      var lagretFarskapserklaeringUtenPadesUrl = persistenceService.lagreFarskapserklaering(farskapserklaeringUtenPadesUrl);

      // when
      var farskapserklaeringerEtterRedirect = persistenceService.henteFarskapserklaeringerEtterRedirect(MOR.getFoedselsnummer(), Forelderrolle.MOR, KjoennTypeDto.KVINNE).stream().findFirst().get();

      // then
      assertAll(
          () -> assertNull(farskapserklaeringerEtterRedirect.getDokument().getPadesUrl(), "PAdES-URL skal ikke være satt i farskapserklæring i det mor redirektes tilbake til farskapsportalen etter utført signering"),
          () -> assertEquals(FARSKAPSERKLAERING.getMor().getFoedselsnummer(), farskapserklaeringerEtterRedirect.getMor().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getFar().getFoedselsnummer(), farskapserklaeringerEtterRedirect.getFar().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getBarn().getTermindato(), farskapserklaeringerEtterRedirect.getBarn().getTermindato())
      );

      farskapserklaeringDao.delete(lagretFarskapserklaeringUtenPadesUrl);
    }

    @Test
    @DisplayName("Skal hente farskapserklæring i forbindelse med fars redirect fra signeringsløsningen")
    void skalHenteFarskapserklaeringEtterRedirectForFar() {

      // given default farskapserklæering, when
      var farskapserklaeringerEtterRedirect = persistenceService.henteFarskapserklaeringerEtterRedirect(FAR.getFoedselsnummer(), Forelderrolle.FAR, KjoennTypeDto.MANN).stream().findFirst().get();

      // then
      assertAll(
          () -> assertNotNull(farskapserklaeringerEtterRedirect.getDokument().getPadesUrl(), "PAdES-URL skal være satt i farskapserklæring i det far redirektes tilbake til farskapsportalen etter utført signering"),
          () -> assertEquals(FARSKAPSERKLAERING.getMor().getFoedselsnummer(), farskapserklaeringerEtterRedirect.getMor().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getFar().getFoedselsnummer(), farskapserklaeringerEtterRedirect.getFar().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getBarn().getTermindato(), farskapserklaeringerEtterRedirect.getBarn().getTermindato())
      );

    }

    @Test
    @DisplayName("Skal hente lagret farskapserklæring")
    void skalHenteLagretFarskapserklaering() {
      var hentedeFarskapserklaeringer =
          persistenceService.henteFarskapserklaeringer(FAR.getFoedselsnummer());

      var hentetFarskapserklaering =
          hentedeFarskapserklaeringer.stream()
              .filter(f -> FAR.getFoedselsnummer().equals(f.getFar().getFoedselsnummer()))
              .findFirst()
              .get();

      assertAll(
          () ->
              assertEquals(
                  FARSKAPSERKLAERING.getFar().getFoedselsnummer(),
                  hentetFarskapserklaering.getFar().getFoedselsnummer()),
          () ->
              assertEquals(
                  FARSKAPSERKLAERING.getBarn().getTermindato(),
                  hentetFarskapserklaering.getBarn().getTermindato()));
    }

    @Test
    @DisplayName("Skal hente lagret barn")
    void skalHenteLagretBarn() {
      var hentetBarn = persistenceService.henteBarn(lagretFarskapserklaering.getBarn().getId());
      assertEquals(lagretFarskapserklaering.getBarn().getTermindato(), hentetBarn.getTermindato());
    }

    @Test
    @DisplayName("Skal hente lagret mor")
    void skalHenteLagretMor() {
      var hentetMor = persistenceService.henteForelder(lagretFarskapserklaering.getMor().getId());
      assertEquals(
          lagretFarskapserklaering.getMor().getFoedselsnummer(), hentetMor.getFoedselsnummer());
    }

    @Test
    @DisplayName("Skal hente lagret far")
    void skalHenteLagretFar() {
      var hentetFar = persistenceService.henteForelder(lagretFarskapserklaering.getFar().getId());
      assertEquals(
          lagretFarskapserklaering.getFar().getFoedselsnummer(), hentetFar.getFoedselsnummer());
    }

    @Test
    @DisplayName("Skal hente lagret redirectUrl for far")
    void skalHenteLagretRedirectUrlForFar() {
      var hentetRedirectUrlFar =
          persistenceService.henteRedirectUrl(
              lagretFarskapserklaering.getDokument().getRedirectUrlFar().getId());
      assertEquals(
          lagretFarskapserklaering.getDokument().getRedirectUrlFar().getRedirectUrl(),
          hentetRedirectUrlFar.getRedirectUrl());
    }

    @Test
    @DisplayName("Skal hente lagret redirectUrl for mor")
    void skalHenteLagretRedirectUrlForMor() {
      var hentetRedirectUrlMor =
          persistenceService.henteRedirectUrl(
              lagretFarskapserklaering.getDokument().getRedirectUrlMor().getId());
      assertEquals(
          lagretFarskapserklaering.getDokument().getRedirectUrlMor().getRedirectUrl(),
          hentetRedirectUrlMor.getRedirectUrl());
    }
  }
}
