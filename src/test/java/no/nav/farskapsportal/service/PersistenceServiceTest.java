package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.ValideringException;
import no.nav.farskapsportal.persistence.dao.BarnDao;
import no.nav.farskapsportal.persistence.dao.DokumentDao;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.StatusKontrollereFar;
import no.nav.farskapsportal.util.MappingUtil;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PersistenceServiceTest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class PersistenceServiceTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto UFOEDT_BARN = henteBarnUtenFnr(17);
  private static final BarnDto NYFOEDT_BARN = henteBarnMedFnr(LocalDate.now().minusWeeks(2));
  private static final FarskapserklaeringDto FARSKAPSERKLAERING = henteFarskapserklaering(MOR, FAR, UFOEDT_BARN);

  @MockBean
  private PersonopplysningService personopplysningServiceMock;
  @Autowired
  private PersistenceService persistenceService;
  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;
  @Autowired
  private BarnDao barnDao;
  @Autowired
  private ForelderDao forelderDao;
  @Autowired
  private DokumentDao dokumentDao;
  @Autowired
  private StatusKontrollereFarDao statusKontrollereFarDao;
  @Autowired
  private MappingUtil mappingUtil;

  @Test
  void skalSetteAntallFeiledeForseokTilEnDersomTidspunktForNullstillingErNaadd() {

  }

  @Nested
  @DisplayName("Lagre")
  @DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
  @AutoConfigureTestDatabase(replace = Replace.ANY)
  class Lagre {

    @Test
    @DisplayName("Lagre barn")
    void lagreBarn() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();

      // given, when
      var barn = persistenceService.lagreBarn(UFOEDT_BARN);
      var barnReturnert = barnDao.findById(barn.getId()).get();

      // then
      assertEquals(UFOEDT_BARN.getTermindato(), barnReturnert.getTermindato());

      // rydde test data
      barnDao.delete(barnReturnert);
    }

    @Test
    @DisplayName("Lagre forelder")
    void lagreForelder() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();

      // given, when
      var lagretMor = persistenceService.lagreForelder(MOR);

      var forelder = forelderDao.findById(lagretMor.getId()).get();

      // then
      assertEquals(MOR.getFoedselsnummer(), forelder.getFoedselsnummer());

      // rydde test data
      forelderDao.delete(lagretMor);
    }

    @Test
    @DisplayName("Lagre dokument")
    void lagreDokument() throws URISyntaxException {

      // given
      var redirectUrlMor = new URI("https://esignering.no/redirect-mor");
      var redirectUrlFar = new URI("https://esignering.no/redirect-far");

      var dokument = DokumentDto.builder().dokumentnavn("farskapserklaring.pdf").padesUrl(new URI("")).redirectUrlMor(redirectUrlMor)
          .redirectUrlFar(redirectUrlFar).build();

      // when
      var lagretDokument = persistenceService.lagreDokument(dokument);

      var hentetDokument = dokumentDao.findById(lagretDokument.getId()).get();

      // then
      assertEquals(dokument.getDokumentnavn(), hentetDokument.getDokumentnavn());

      // clean up test data
      dokumentDao.delete(lagretDokument);
    }

    @Test
    @DisplayName("Lagre farskapserklæring")
    void lagreFarskapserklaering() {

      // given
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // when
      var lagretFarskapserklaering = persistenceService.lagreFarskapserklaering(FARSKAPSERKLAERING);

      var hentetFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId()).get();

      // then
      assertEquals(lagretFarskapserklaering, hentetFarskapserklaering, "Farskapserklæringen som ble lagret er lik den som ble hentet");

      // rydde test data
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @Test
    @DisplayName("Skal ikke lagre ny farskapserklæring dersom tilsvarende allerede eksisterer")
    void skalIkkeLagreNyFarskapserklaeringDersomTilsvarendeAlleredeEksisterer() {

      // given
      farskapserklaeringDao.save(mappingUtil.toEntity(FARSKAPSERKLAERING));

      // when, then
      assertThrows(ValideringException.class, () -> persistenceService.lagreFarskapserklaering(FARSKAPSERKLAERING));
    }

    @Test
    @DisplayName("Skal lagre ny instans av StatusKontrollereFar")
    void skalLagreNyInstansAvStatuskontrollereFar() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var antallDagerTilNullsettingAvForsoek = 1;
      var navnDtoMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(navnDtoMor);

      // when
      var lagretStatusKontrollereFar = persistenceService.oppdatereStatusKontrollereFar(MOR.getFoedselsnummer(), antallDagerTilNullsettingAvForsoek);

      var hentetStatusKontrollereFar = statusKontrollereFarDao.findById(lagretStatusKontrollereFar.getId());

      // then
      assertThat(lagretStatusKontrollereFar).isEqualTo(hentetStatusKontrollereFar.get());

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
      var farskapserklaering = farskapserklaeringDao
          .henteUnikFarskapserklaering(FARSKAPSERKLAERING.getMor().getFoedselsnummer(), FARSKAPSERKLAERING.getFar().getFoedselsnummer(),
              FARSKAPSERKLAERING.getBarn().getTermindato());
      var padesUrl = farskapserklaering.get().getDokument().getPadesUrl();
      farskapserklaering.get().getDokument().setPadesUrl(null);
      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaering.get());

      assertAll(() -> assertNull(lagretFarskapserklaering.getDokument().getPadesUrl()),
          () -> assertNull(lagretFarskapserklaering.getDokument().getSignertAvMor()),
          () -> assertNull(lagretFarskapserklaering.getDokument().getSignertAvFar()));

      // when
      var farskapserklaeringerEtterRedirect = persistenceService
          .henteFarskapserklaeringerEtterRedirect(MOR.getFoedselsnummer(), Forelderrolle.MOR, KjoennType.KVINNE).stream().findFirst().get();

      // then
      assertAll(() -> assertNull(farskapserklaeringerEtterRedirect.getDokument().getPadesUrl(),
          "PAdES-URL skal ikke være satt i farskapserklæring i det mor redirektes tilbake til farskapsportalen etter utført signering"),
          () -> assertEquals(FARSKAPSERKLAERING.getMor().getFoedselsnummer(), farskapserklaeringerEtterRedirect.getMor().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getFar().getFoedselsnummer(), farskapserklaeringerEtterRedirect.getFar().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getBarn().getTermindato(), farskapserklaeringerEtterRedirect.getBarn().getTermindato()));

      // Clean up test data
      farskapserklaering.get().getDokument().setPadesUrl(padesUrl);
      farskapserklaeringDao.save(farskapserklaering.get());
    }

    @Test
    @DisplayName("Skal hente farskapserklæring i forbindelse med fars redirect fra signeringsløsningen")
    void skalHenteFarskapserklaeringEtterRedirectForFar() {

      // given default farskapserklæering, when
      var farskapserklaeringerEtterRedirect = persistenceService
          .henteFarskapserklaeringerEtterRedirect(FAR.getFoedselsnummer(), Forelderrolle.FAR, KjoennType.MANN).stream().findFirst().get();

      // then
      assertAll(() -> assertNotNull(farskapserklaeringerEtterRedirect.getDokument().getPadesUrl(),
          "PAdES-URL skal være satt i farskapserklæring i det far redirektes tilbake til farskapsportalen etter utført signering"),
          () -> assertEquals(FARSKAPSERKLAERING.getMor().getFoedselsnummer(), farskapserklaeringerEtterRedirect.getMor().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getFar().getFoedselsnummer(), farskapserklaeringerEtterRedirect.getFar().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getBarn().getTermindato(), farskapserklaeringerEtterRedirect.getBarn().getTermindato()));
    }

    @Test
    @DisplayName("Skal hente lagret farskapserklæring")
    void skalHenteLagretFarskapserklaering() {
      var hentedeFarskapserklaeringer = persistenceService.henteFarskapserklaeringer(FAR.getFoedselsnummer());

      var hentetFarskapserklaering = hentedeFarskapserklaeringer.stream().filter(f -> FAR.getFoedselsnummer().equals(f.getFar().getFoedselsnummer()))
          .findFirst().get();

      assertAll(() -> assertEquals(FARSKAPSERKLAERING.getFar().getFoedselsnummer(), hentetFarskapserklaering.getFar().getFoedselsnummer()),
          () -> assertEquals(FARSKAPSERKLAERING.getBarn().getTermindato(), hentetFarskapserklaering.getBarn().getTermindato()));
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
      assertEquals(lagretFarskapserklaering.getMor().getFoedselsnummer(), hentetMor.getFoedselsnummer());
    }

    @Test
    @DisplayName("Skal hente lagret far")
    void skalHenteLagretFar() {
      var hentetFar = persistenceService.henteForelder(lagretFarskapserklaering.getFar().getId());
      assertEquals(lagretFarskapserklaering.getFar().getFoedselsnummer(), hentetFar.getFoedselsnummer());
    }

    @Test
    @DisplayName("Skal hente lagret statusKontrollereFar")
    void skalHenteLagretStatusKontrollereFar() {

      // given
      var foerTidspunktForSisteFeiledeForsoek = LocalDateTime.now();
      var antallDagerTilNullsettingAvForsoek = 1;
      persistenceService.oppdatereStatusKontrollereFar(MOR.getFoedselsnummer(), antallDagerTilNullsettingAvForsoek);

      // when
      var hentetStatusLagreKontrollereFar = persistenceService.henteStatusKontrollereFar(MOR.getFoedselsnummer());

      // then
      var etterTidspunktForSisteFeiledeForsoek = LocalDateTime.now();
      assertAll(() -> assertThat(hentetStatusLagreKontrollereFar).isPresent(),
          () -> assertThat(hentetStatusLagreKontrollereFar.get().getTidspunktSisteFeiledeForsoek()).isBefore(etterTidspunktForSisteFeiledeForsoek),
          () -> assertThat(hentetStatusLagreKontrollereFar.get().getTidspunktSisteFeiledeForsoek()).isAfter(foerTidspunktForSisteFeiledeForsoek));
    }
  }

  @Nested
  @DisplayName("OppdatereStatusKontrollereFar")
  class OppdatereStatusKontrollereFar {

    @Test
    void skalOppretteNyOppdatereStatusKontrollereFarDersomKontrollFarFeilerForFoersteGang() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var antallDagerTilNullstilling = 1;
      var tidspunktFoerLogging = LocalDateTime.now();
      forelderDao.save(Forelder.builder().foedselsnummer(MOR.getFoedselsnummer()).fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build());

      // when
      var statusKontrollereFar = persistenceService.oppdatereStatusKontrollereFar(MOR.getFoedselsnummer(), antallDagerTilNullstilling);

      // then
      var tidspunktEtterLogging = LocalDateTime.now();
      assertAll(() -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(1),
          () -> assertThat(statusKontrollereFar.getTidspunktSisteFeiledeForsoek()).isAfter(tidspunktFoerLogging),
          () -> assertThat(statusKontrollereFar.getTidspunktSisteFeiledeForsoek()).isBefore(tidspunktEtterLogging),
          () -> assertThat(statusKontrollereFar.getMor().getFoedselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
          () -> assertThat(statusKontrollereFar.getMor().getFornavn()).isEqualTo(MOR.getFornavn()),
          () -> assertThat(statusKontrollereFar.getMor().getEtternavn()).isEqualTo(MOR.getEtternavn()));
    }

    @Test
    void skalLeggeInnMorSomForelderDersomHunIkkeEksistererIDatabasenVedOpprettelseAvNyttInnslagIStatusKontrollereFar() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var antallDagerTilNullstilling = 1;
      var tidspunktFoerLogging = LocalDateTime.now();
      var navnDtoMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(navnDtoMor);

      // when
      var statusKontrollereFar = persistenceService.oppdatereStatusKontrollereFar(MOR.getFoedselsnummer(), antallDagerTilNullstilling);

      // then
      var tidspunktEtterLogging = LocalDateTime.now();
      assertAll(() -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(1),
          () -> assertThat(statusKontrollereFar.getTidspunktSisteFeiledeForsoek()).isAfter(tidspunktFoerLogging),
          () -> assertThat(statusKontrollereFar.getTidspunktSisteFeiledeForsoek()).isBefore(tidspunktEtterLogging),
          () -> assertThat(statusKontrollereFar.getMor().getFoedselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
          () -> assertThat(statusKontrollereFar.getMor().getFornavn()).isEqualTo(MOR.getFornavn()),
          () -> assertThat(statusKontrollereFar.getMor().getEtternavn()).isEqualTo(MOR.getEtternavn()));

    }

    @Test
    void skalInkrementereAntallFeiledeForsoekDersomTidspunktForNullstillingIkkeErNaadd() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var antallDagerTilNullstilling = 1;
      var tidspunktForForrigeFeil = LocalDateTime.now();
      var eksisterendeStatusKontrollereFar = lagreStatusKontrollereFarMedMor(MOR, 2, tidspunktForForrigeFeil);

      // when
      var statusKontrollereFar = persistenceService.oppdatereStatusKontrollereFar(MOR.getFoedselsnummer(), antallDagerTilNullstilling);

      // then
      var tidspunktEtterLogging = LocalDateTime.now();
      assertAll(
          () -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(eksisterendeStatusKontrollereFar.getAntallFeiledeForsoek() + 1),
          () -> assertThat(statusKontrollereFar.getTidspunktSisteFeiledeForsoek()).isAfter(tidspunktForForrigeFeil),
          () -> assertThat(statusKontrollereFar.getTidspunktSisteFeiledeForsoek()).isBefore(tidspunktEtterLogging),
          () -> assertThat(statusKontrollereFar.getMor().getFoedselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
          () -> assertThat(statusKontrollereFar.getMor().getFornavn()).isEqualTo(MOR.getFornavn()),
          () -> assertThat(statusKontrollereFar.getMor().getEtternavn()).isEqualTo(MOR.getEtternavn()));
    }

    @Test
    void skalSetteAntallForsoekTilEnVedFeilDersomTidspunktForNullstillingErNaadd() {
      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var antallDagerTilNullstilling = 0;
      var tidspunktForForrigeFeil = LocalDateTime.now();
      lagreStatusKontrollereFarMedMor(MOR, 4, tidspunktForForrigeFeil);

      // when
      var statusKontrollereFar = persistenceService.oppdatereStatusKontrollereFar(MOR.getFoedselsnummer(), antallDagerTilNullstilling);

      // then
      var tidspunktEtterLogging = LocalDateTime.now();
      assertAll(() -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(1),
          () -> assertThat(statusKontrollereFar.getTidspunktSisteFeiledeForsoek()).isAfter(tidspunktForForrigeFeil),
          () -> assertThat(statusKontrollereFar.getTidspunktSisteFeiledeForsoek()).isBefore(tidspunktEtterLogging),
          () -> assertThat(statusKontrollereFar.getMor().getFoedselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
          () -> assertThat(statusKontrollereFar.getMor().getFornavn()).isEqualTo(MOR.getFornavn()),
          () -> assertThat(statusKontrollereFar.getMor().getEtternavn()).isEqualTo(MOR.getEtternavn()));
    }

    private StatusKontrollereFar lagreStatusKontrollereFarMedMor(ForelderDto mor, int antallFeil, LocalDateTime tidspunktSisteFeil) {
      var morEntitet = Forelder.builder().foedselsnummer(mor.getFoedselsnummer()).fornavn(mor.getFornavn()).etternavn(mor.getEtternavn()).build();
      return statusKontrollereFarDao.save(
          StatusKontrollereFar.builder().mor(morEntitet).antallFeiledeForsoek(antallFeil).tidspunktSisteFeiledeForsoek(tidspunktSisteFeil).build());
    }
  }

  @Nested
  @DisplayName("IngenKonfliktMedEksisterendeFarskapserklaeringer")
  class IngenKonfliktMedEksisterendeFarskapserklaeringer {

    @Test
    void skalKasteValideringExceptionDersomMorHarEksisterendeFarskapserklaeringOgOppretterNyMedTermindato() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      persistenceService.lagreFarskapserklaering(FARSKAPSERKLAERING);

      // when, then
      assertThrows(ValideringException.class,
          () -> persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(MOR.getFoedselsnummer(), FAR.getFoedselsnummer(), UFOEDT_BARN));
    }

    @Test
    void skalKasteValideringExceptionDersomNyfoedtBarnInngaarIEksisterendeFarskapserklaering() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var fnrMorUtenEksisterendeFarskapserklaering = LocalDate.now().minusYears(29).format(DateTimeFormatter.ofPattern("ddMMyy")) + "12245";
      var farskapserklaering = FarskapserklaeringDto.builder().barn(NYFOEDT_BARN).mor(MOR).far(FAR).dokument(FARSKAPSERKLAERING.getDokument())
          .build();
      persistenceService.lagreFarskapserklaering(farskapserklaering);

      // when, then
      assertThrows(ValideringException.class, () -> persistenceService
          .ingenKonfliktMedEksisterendeFarskapserklaeringer(fnrMorUtenEksisterendeFarskapserklaering, FAR.getFoedselsnummer(), NYFOEDT_BARN));
    }

    @Test
    void skalIkkeKasteExceptionDersomMorIkkeHarEksisterendeFarskapserklaering() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given, when, then
      assertDoesNotThrow(
          () -> persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(MOR.getFoedselsnummer(), FAR.getFoedselsnummer(), UFOEDT_BARN));
    }
  }
}
