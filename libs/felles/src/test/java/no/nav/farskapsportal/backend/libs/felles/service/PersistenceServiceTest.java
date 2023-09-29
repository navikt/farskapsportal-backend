package no.nav.farskapsportal.backend.libs.felles.service;

import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import no.nav.farskapsportal.backend.libs.dto.BarnDto;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.entity.*;
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.exception.ValideringException;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.DokumentDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PersistenceServiceTest")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalFellesTestConfig.class)
@ActiveProfiles(FarskapsportalFellesConfig.PROFILE_TEST)
@AutoConfigureWireMock(port = 0)
public class PersistenceServiceTest {

  private static final Forelder MOR = TestUtils.henteForelder(Forelderrolle.MOR);
  private static final Forelder FAR = TestUtils.henteForelder(Forelderrolle.FAR);
  private static final NavnDto NAVN_FAR =
      NavnDto.builder().fornavn("Fetter").etternavn("Anton").build();
  private static final Barn UFOEDT_BARN = TestUtils.henteBarnUtenFnr(17);
  private static final Barn NYFOEDT_BARN = TestUtils.henteBarnMedFnr(LocalDate.now().minusWeeks(2));

  @Value("${wiremock.server.port}")
  String wiremockPort;

  private @MockBean GcpStorageManager gcpStorageManager;
  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @Autowired ForelderDao forelderDao;
  private @Autowired DokumentDao dokumentDao;
  private @Autowired StatusKontrollereFarDao statusKontrollereFarDao;
  private @Autowired ModelMapper modelMapper;
  private @Autowired FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Nested
  @DisplayName("Lagre")
  @DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
  @AutoConfigureTestDatabase(replace = Replace.ANY)
  class Lagre {

    @BeforeEach
    void resetGeneratedValue() {
      MOR.setId(0);
      FAR.setId(0);
      UFOEDT_BARN.setId(0);
    }

    @Test
    @DisplayName("Lagre dokument")
    void lagreDokument() {

      // given
      var redirectUrlMor = "https://esignering.no/redirect-mor";
      var redirectUrlFar = "https://esignering.no/redirect-far";

      var dokument =
          Dokument.builder()
              .navn("farskapserklaring.pdf")
              .signeringsinformasjonMor(
                  Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build())
              .signeringsinformasjonFar(
                  Signeringsinformasjon.builder().redirectUrl(redirectUrlFar).build())
              .build();

      // when
      var lagretDokument = dokumentDao.save(dokument);
      var hentetDokument = dokumentDao.findById(lagretDokument.getId()).get();

      // then
      Assertions.assertEquals(dokument.getNavn(), hentetDokument.getNavn());

      // clean up test data
      dokumentDao.delete(lagretDokument);
    }

    @Test
    @DisplayName("Lagre farskapserklæring")
    void lagreFarskapserklaering() {

      // given
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // when
      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(
              henteFarskapserklaering(MOR, FAR, UFOEDT_BARN));

      var hentetFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId()).get();

      // then
      Assertions.assertEquals(
          lagretFarskapserklaering,
          hentetFarskapserklaering,
          "Farskapserklæringen som ble lagret er lik den som ble hentet");

      // rydde test data
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @Test
    void lagreFarskapserklaeringMedSammeMorFarOgBarnSomIDeaktivertFarskapserklaering() {

      // given
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var deaktivertFarskapserklaeringMedSammeMorFarOgBarn =
          henteFarskapserklaering(MOR, FAR, NYFOEDT_BARN);
      var lagretDeaktivertFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(
              deaktivertFarskapserklaeringMedSammeMorFarOgBarn);
      lagretDeaktivertFarskapserklaering.setDeaktivert(LocalDateTime.now());
      persistenceService.oppdatereFarskapserklaering(lagretDeaktivertFarskapserklaering);

      var duplikatAktivFarskapserklaering = henteFarskapserklaering(MOR, FAR, NYFOEDT_BARN);

      // when
      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(duplikatAktivFarskapserklaering);

      var hentetFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId()).get();

      // then
      Assertions.assertEquals(
          lagretFarskapserklaering,
          hentetFarskapserklaering,
          "Farskapserklæringen som ble lagret er lik den som ble hentet");

      // rydde test data
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @Test
    @DisplayName("Skal ikke lagre ny farskapserklæring dersom tilsvarende allerede eksisterer")
    void skalIkkeLagreNyFarskapserklaeringDersomTilsvarendeAlleredeEksisterer() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // given
      farskapserklaeringDao.save(
          henteFarskapserklaering(
              TestUtils.henteForelder(Forelderrolle.MOR),
              TestUtils.henteForelder(Forelderrolle.FAR),
              TestUtils.henteBarnUtenFnr(17)));

      // when, then
      assertThrows(
          ValideringException.class,
          () ->
              persistenceService.lagreNyFarskapserklaering(
                  henteFarskapserklaering(MOR, FAR, UFOEDT_BARN)));
    }

    @Test
    @DisplayName("Skal lagre ny instans av StatusKontrollereFar")
    void skalLagreNyInstansAvStatuskontrollereFar() {

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var antallDagerTilNullsettingAvForsoek = 1;

      // when
      var lagretStatusKontrollereFar =
          persistenceService.oppdatereStatusKontrollereFar(
              MOR.getFoedselsnummer(),
              NAVN_FAR.sammensattNavn(),
              "Ronald McDonald",
              antallDagerTilNullsettingAvForsoek,
              farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());

      var hentetStatusKontrollereFar =
          statusKontrollereFarDao.findById(lagretStatusKontrollereFar.getId());

      // then
      assertAll(
          () ->
              assertThat(lagretStatusKontrollereFar.getAntallFeiledeForsoek())
                  .isEqualTo(hentetStatusKontrollereFar.get().getAntallFeiledeForsoek()),
          () ->
              assertThat(lagretStatusKontrollereFar.getTidspunktForNullstilling())
                  .isEqualToIgnoringSeconds(
                      hentetStatusKontrollereFar.get().getTidspunktForNullstilling()),
          () ->
              assertThat(lagretStatusKontrollereFar.getMor().getFoedselsnummer())
                  .isEqualTo(hentetStatusKontrollereFar.get().getMor().getFoedselsnummer()));
    }
  }

  @Nested
  @DisplayName("Hente")
  @TestInstance(Lifecycle.PER_CLASS)
  class Hente {

    @BeforeEach
    void ryddeTestdata() {
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
    }

    private Farskapserklaering lagreFarskapserklaering() {
      var farskapserklaering =
          henteFarskapserklaering(
              TestUtils.henteForelder(Forelderrolle.MOR),
              TestUtils.henteForelder(Forelderrolle.FAR),
              TestUtils.henteBarnUtenFnr(17));
      return farskapserklaeringDao.save(farskapserklaering);
    }

    private Farskapserklaering lagreFarskapserklaeringSignertAvMor() {
      return lagreFarskapserklaeringSignertAvMor(LocalDateTime.now().minusMinutes(15));
    }

    private Farskapserklaering lagreFarskapserklaeringSignertAvMor(
        LocalDateTime signeringstidspunkt) {
      var farskapserklaeringSignertAvMor = lagreFarskapserklaering();
      farskapserklaeringSignertAvMor
          .getDokument()
          .setPadesUrl("https://esignering.posten.no/" + MOR.getFoedselsnummer() + "/pades");
      farskapserklaeringSignertAvMor
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(signeringstidspunkt);
      return farskapserklaeringDao.save(farskapserklaeringSignertAvMor);
    }

    @Test
    @DisplayName("Skal hente farskapserklæring som venter på far")
    void skalHenteFarskapserklaeringSomVenterPaaFar() {

      // given
      lagreFarskapserklaeringSignertAvMor();

      // when
      var hentedeFarskapserklaeringer =
          persistenceService.henteFarsErklaeringer(FAR.getFoedselsnummer());

      // then
      var hentetFarskapserklaering =
          hentedeFarskapserklaeringer.stream()
              .filter(f -> FAR.getFoedselsnummer().equals(f.getFar().getFoedselsnummer()))
              .findFirst()
              .get();

      assertAll(
          () ->
              assertEquals(
                  henteFarskapserklaering(MOR, FAR, UFOEDT_BARN).getFar().getFoedselsnummer(),
                  hentetFarskapserklaering.getFar().getFoedselsnummer()),
          () ->
              assertEquals(
                  henteFarskapserklaering(MOR, FAR, UFOEDT_BARN).getBarn().getTermindato(),
                  hentetFarskapserklaering.getBarn().getTermindato()),
          () -> assertThat(hentetFarskapserklaering.getDeaktivert()).isNull());
    }

    @Test
    @DisplayName("Skal hente lagret mor")
    void skalHenteLagretMor() {

      // given
      var lagretFarskapserklaering = lagreFarskapserklaering();

      // when
      var hentetMor = persistenceService.henteForelder(lagretFarskapserklaering.getMor().getId());

      // then
      assertEquals(
          lagretFarskapserklaering.getMor().getFoedselsnummer(), hentetMor.getFoedselsnummer());
    }

    @Test
    @DisplayName("Skal hente lagret far")
    void skalHenteLagretFar() {

      // given
      var lagretFarskapserklaering = lagreFarskapserklaering();

      // when
      var hentetFar = persistenceService.henteForelder(lagretFarskapserklaering.getFar().getId());

      // then
      assertEquals(
          lagretFarskapserklaering.getFar().getFoedselsnummer(), hentetFar.getFoedselsnummer());
    }

    @Test
    @DisplayName("Skal hente lagret statusKontrollereFar")
    void skalHenteLagretStatusKontrollereFar() {

      // given
      var antallDagerTilNullsettingAvForsoek = 1;
      var foerTidspunktForNullstilling =
          LocalDateTime.now().plusDays(antallDagerTilNullsettingAvForsoek);
      persistenceService.oppdatereStatusKontrollereFar(
          MOR.getFoedselsnummer(),
          NAVN_FAR.sammensattNavn(),
          NAVN_FAR.sammensattNavn(),
          antallDagerTilNullsettingAvForsoek,
          farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());

      // when
      var hentetStatusLagreKontrollereFar =
          persistenceService.henteStatusKontrollereFar(MOR.getFoedselsnummer());

      // then
      var etterTidspunktForNullstilling =
          LocalDateTime.now().plusDays(antallDagerTilNullsettingAvForsoek);

      assertAll(
          () -> assertThat(hentetStatusLagreKontrollereFar).isPresent(),
          () ->
              assertThat(hentetStatusLagreKontrollereFar.get().getTidspunktForNullstilling())
                  .isBefore(etterTidspunktForNullstilling),
          () ->
              assertThat(hentetStatusLagreKontrollereFar.get().getTidspunktForNullstilling())
                  .isAfter(foerTidspunktForNullstilling));
    }

    @Test
    void skalHenteFarskapserklaeringForIdSomFinnesIDatabasen() {

      // given
      var lagretFarskapserklaering = lagreFarskapserklaering();
      assertNotNull(lagretFarskapserklaering);

      // when
      var farskapserklaering =
          persistenceService.henteFarskapserklaeringForId(lagretFarskapserklaering.getId());

      // then
      assertAll(
          () ->
              assertThat(
                      lagretFarskapserklaering
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getUndertegnerUrl())
                  .isEqualTo(
                      farskapserklaering
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getUndertegnerUrl()),
          () -> assertThat(lagretFarskapserklaering.getDeaktivert()).isNull());
    }

    @Test
    void skalHenteAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag() {

      // given
      var lagretFarskapserklaering =
          lagreFarskapserklaeringSignertAvMor(
              ZonedDateTime.now().minusDays(41).with(ChronoField.HOUR_OF_DAY, 0).toLocalDateTime());
      assertNotNull(lagretFarskapserklaering);

      // when
      var idTilFarskapserklaeringer =
          persistenceService.henteIdTilAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag(
              LocalDateTime.now().minusDays(40));

      // then
      assertAll(
          () -> assertThat(idTilFarskapserklaeringer.size()).isEqualTo(1),
          () ->
              assertThat(idTilFarskapserklaeringer.stream().findFirst().get())
                  .isEqualTo(lagretFarskapserklaering.getId()));
    }

    @Test
    void skalIkkeHenteAktiveFarskapserklaeringerUtenUtgaatteSigneringsoppdrag() {

      // given
      var lagretFarskapserklaering =
          lagreFarskapserklaeringSignertAvMor(LocalDate.now().minusDays(2).atStartOfDay());
      assertNotNull(lagretFarskapserklaering);

      // when
      var farskapserklaeringer =
          persistenceService.henteIdTilAktiveFarskapserklaeringerMedUtgaatteSigneringsoppdrag(
              LocalDateTime.now().minusDays(40));

      // then
      assertThat(farskapserklaeringer.size()).isEqualTo(0);
    }

    @Test
    @DisplayName(
        "Skal kaste RessursIkkeFunnetException ved henting av undertegnerUrl dersom farskapserklaering ikke finnes")
    void
        skalKasteRessursIkkeFunnetExceptionVedHentingAvUndertegnerurlDersomFarskapserklaeringIkkeFinnes() {

      // given
      var lagretFarskapserklaering = lagreFarskapserklaering();
      assertNotNull(lagretFarskapserklaering);

      // when, then
      assertThrows(
          RessursIkkeFunnetException.class,
          () ->
              persistenceService.henteFarskapserklaeringForId(
                  lagretFarskapserklaering.getId() + 1));
    }

    @Test
    void
        skalHenteFarskapserklaeringerForDokumentslettingDersomDeaktiveringOgSendtTilSkatttidspunktErFoerGrenseverdi() {

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              TestUtils.henteForelder(Forelderrolle.MOR),
              TestUtils.henteForelder(Forelderrolle.FAR),
              TestUtils.henteBarnUtenFnr(-17));

      var grensetidspunkt = LocalDateTime.now().minusMonths(12);

      farskapserklaering.setDeaktivert(grensetidspunkt.minusHours(1));
      farskapserklaering.setSendtTilSkatt(grensetidspunkt.minusHours(1));
      farskapserklaering
          .getDokument()
          .setDokumentinnhold(
              Dokumentinnhold.builder().innhold("En farskapserklæring".getBytes()).build());

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaering);

      // when
      var idTilFarskapserklaeringer =
          persistenceService.henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(
              grensetidspunkt, grensetidspunkt);

      // then
      assertAll(
          () -> assertThat(idTilFarskapserklaeringer.size()).isEqualTo(1),
          () ->
              assertThat(idTilFarskapserklaeringer.stream().findFirst().get())
                  .isEqualTo(lagretFarskapserklaering.getId()));
    }

    @Test
    void
        skalIkkeHenteFarskapserklaeringerForDokumentslettingDersomDeaktiveringOgSendtTilSkatttidspunktErEtterGrenseverdi() {

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              TestUtils.henteForelder(Forelderrolle.MOR),
              TestUtils.henteForelder(Forelderrolle.FAR),
              TestUtils.henteBarnUtenFnr(-17));

      var grensetidspunkt = LocalDateTime.now().minusMonths(12);

      farskapserklaering.setDeaktivert(grensetidspunkt.plusHours(1));
      farskapserklaering.setSendtTilSkatt(grensetidspunkt.plusHours(1));

      farskapserklaeringDao.save(farskapserklaering);

      // when
      var idTilFarskapserklaeringer =
          persistenceService.henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(
              grensetidspunkt, grensetidspunkt);

      // then
      assertAll(() -> assertThat(idTilFarskapserklaeringer.size()).isEqualTo(0));
    }

    @Test
    void
        skalIkkeHenteFarskapserklaeringerForDokumentslettingDersomDeaktiveringOgSendtTilSkatttidspunktMangler() {

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              TestUtils.henteForelder(Forelderrolle.MOR),
              TestUtils.henteForelder(Forelderrolle.FAR),
              TestUtils.henteBarnUtenFnr(-17));

      var grensetidspunkt = LocalDateTime.now().minusMonths(12);

      farskapserklaering.setDeaktivert(null);
      farskapserklaering.setSendtTilSkatt(null);

      farskapserklaeringDao.save(farskapserklaering);

      // when
      var idTilFarskapserklaeringer =
          persistenceService.henteIdTilFarskapserklaeringerDokumenterSkalSlettesFor(
              grensetidspunkt, grensetidspunkt);

      // then
      assertAll(() -> assertThat(idTilFarskapserklaeringer.size()).isEqualTo(0));
    }
  }

  @Nested
  @DisplayName("Slette")
  class Slette {

    private Farskapserklaering lagreFarskapserklaering() {
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      var farskapserklaering =
          henteFarskapserklaering(
              TestUtils.henteForelder(Forelderrolle.MOR),
              TestUtils.henteForelder(Forelderrolle.FAR),
              TestUtils.henteBarnUtenFnr(17));
      farskapserklaering
          .getDokument()
          .setDokumentinnhold(
              Dokumentinnhold.builder().innhold("En farskapserklæring".getBytes()).build());
      return farskapserklaeringDao.save(farskapserklaering);
    }

    @Test
    void skalDeaktivereFarskapserklaeringSomManglerFarsSignatur() {

      // given
      var lagretFarskapserklaering = lagreFarskapserklaering();

      // when
      var erklaeringBleDeaktivert =
          persistenceService.deaktivereFarskapserklaering(lagretFarskapserklaering.getId());

      // then
      assertAll(
          () -> assertThat(erklaeringBleDeaktivert).isTrue(),
          () ->
              assertThrows(
                  RessursIkkeFunnetException.class,
                  () ->
                      persistenceService.henteFarskapserklaeringForId(
                          lagretFarskapserklaering.getId())));
    }

    @Test
    void skalKasteInternFeilExceptionDersomFarskapserklaeringBlirForsoektDeaktivertIkkeFinnes() {

      // given
      var lagretFarskapserklaering = lagreFarskapserklaering();

      // when
      var illegalStateException =
          assertThrows(
              IllegalStateException.class,
              () ->
                  persistenceService.deaktivereFarskapserklaering(
                      lagretFarskapserklaering.getId() + 1));

      // then
      AssertionsForClassTypes.assertThat(illegalStateException.getMessage())
          .isEqualTo("Farskapserklæring ikke funnet");
    }

    @Test
    void skalOppdatereTidspunktForDokumentsletting() {

      // given
      var lagretFarskapserklaering = lagreFarskapserklaering();
      lagretFarskapserklaering.setSendtTilSkatt(LocalDateTime.now());
      lagretFarskapserklaering.setDeaktivert(LocalDateTime.now());
      farskapserklaeringDao.save(lagretFarskapserklaering);

      // when
      persistenceService.sletteDokumentinnhold(lagretFarskapserklaering.getId());

      // then
      var farskapserklaeringMedSlettedeDokumenter =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () ->
              assertThat(farskapserklaeringMedSlettedeDokumenter.get().getDokumenterSlettet())
                  .isNotNull(),
          () ->
              assertThat(
                      farskapserklaeringMedSlettedeDokumenter
                          .get()
                          .getDokument()
                          .getDokumentinnhold()
                          .getInnhold())
                  .isNull());
    }
  }

  @Nested
  @DisplayName("OppdatereStatusKontrollereFar")
  class OppdatereStatusKontrollereFar {

    @BeforeEach
    void ryddeTestdata() {
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
    }

    @Test
    void skalOppretteNyOppdatereStatusKontrollereFarDersomKontrollFarFeilerForFoersteGang() {

      // given
      var antallDagerTilNullstilling = 1;
      var tidspunktForNullstillingFoerLogging =
          LocalDateTime.now().plusDays(antallDagerTilNullstilling);
      forelderDao.save(Forelder.builder().foedselsnummer(MOR.getFoedselsnummer()).build());

      // when
      var statusKontrollereFar =
          persistenceService.oppdatereStatusKontrollereFar(
              MOR.getFoedselsnummer(),
              NAVN_FAR.sammensattNavn(),
              "Ronald McDonald",
              antallDagerTilNullstilling,
              farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());

      // then
      var tidspunktForNullstillingEtterLogging =
          LocalDateTime.now().plusDays(antallDagerTilNullstilling);

      assertAll(
          () -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(1),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isAfter(tidspunktForNullstillingFoerLogging),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isBefore(tidspunktForNullstillingEtterLogging),
          () ->
              assertThat(statusKontrollereFar.getMor().getFoedselsnummer())
                  .isEqualTo(MOR.getFoedselsnummer()));
    }

    @Test
    void
        skalLeggeInnMorSomForelderDersomHunIkkeEksistererIDatabasenVedOpprettelseAvNyttInnslagIStatusKontrollereFar() {

      // given
      var antallDagerTilNullstilling = 1;
      var foerTidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);

      // when
      var statusKontrollereFar =
          persistenceService.oppdatereStatusKontrollereFar(
              MOR.getFoedselsnummer(),
              NAVN_FAR.sammensattNavn(),
              "Ronald McDonald",
              antallDagerTilNullstilling,
              farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());

      // then
      var etterTidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);

      assertAll(
          () -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(1),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isAfter(foerTidspunktForNullstilling),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isBefore(etterTidspunktForNullstilling),
          () ->
              assertThat(statusKontrollereFar.getMor().getFoedselsnummer())
                  .isEqualTo(MOR.getFoedselsnummer()));
    }

    @Test
    void skalReferereTilRiktigForelder() {

      // given
      var antallDagerTilNullstilling = 1;
      var foerTidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);
      forelderDao.save(Forelder.builder().foedselsnummer(MOR.getFoedselsnummer()).build());

      // when
      var statusKontrollereFar =
          persistenceService.oppdatereStatusKontrollereFar(
              MOR.getFoedselsnummer(),
              NAVN_FAR.sammensattNavn(),
              "Ronald McDonald",
              antallDagerTilNullstilling,
              farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());

      // then
      var etterTidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);

      assertAll(
          () -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(1),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isAfter(foerTidspunktForNullstilling),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isBefore(etterTidspunktForNullstilling),
          () ->
              assertThat(statusKontrollereFar.getMor().getFoedselsnummer())
                  .isEqualTo(MOR.getFoedselsnummer()));
    }

    @Test
    void skalIkkeOppdatereDersomAntallForsoekErOverskredetOgFornyelseperiodeIkkeUtloept() {

      // given
      var antallDagerTilNullstilling = 1;
      var foerTidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);
      forelderDao.save(Forelder.builder().foedselsnummer(MOR.getFoedselsnummer()).build());

      // when
      for (int i = 1; i <= farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek(); i++) {
        var statusKontrollereFar =
            persistenceService.oppdatereStatusKontrollereFar(
                MOR.getFoedselsnummer(),
                NAVN_FAR.sammensattNavn(),
                "Ronald McDonald",
                antallDagerTilNullstilling,
                farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());
        int finalI = i;
        assertAll(
            () -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(finalI),
            () ->
                assertThat(statusKontrollereFar.getMor().getFoedselsnummer())
                    .isEqualTo(MOR.getFoedselsnummer()));
      }

      var statusKontrollereFar =
          persistenceService.oppdatereStatusKontrollereFar(
              MOR.getFoedselsnummer(),
              NAVN_FAR.sammensattNavn(),
              "Ronald McDonald",
              antallDagerTilNullstilling,
              farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());

      // then
      var etterTidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);

      assertAll(
          () ->
              assertThat(statusKontrollereFar.getAntallFeiledeForsoek())
                  .isEqualTo(farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek()),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isAfter(foerTidspunktForNullstilling),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isBefore(etterTidspunktForNullstilling),
          () ->
              assertThat(statusKontrollereFar.getMor().getFoedselsnummer())
                  .isEqualTo(MOR.getFoedselsnummer()));
    }

    @Test
    void skalInkrementereAntallFeiledeForsoekDersomTidspunktForNullstillingIkkeErNaadd() {

      // given
      var antallDagerTilNullstilling = 1;
      var tidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);
      var eksisterendeStatusKontrollereFar =
          lagreStatusKontrollereFarMedMor(
              farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek() - 1,
              tidspunktForNullstilling);

      // when
      var statusKontrollereFar =
          persistenceService.oppdatereStatusKontrollereFar(
              MOR.getFoedselsnummer(),
              NAVN_FAR.sammensattNavn(),
              "Ronald McDonald",
              antallDagerTilNullstilling,
              farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());

      // then
      var etterTidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);

      assertAll(
          () ->
              assertThat(statusKontrollereFar.getAntallFeiledeForsoek())
                  .isEqualTo(eksisterendeStatusKontrollereFar.getAntallFeiledeForsoek() + 1),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isBefore(etterTidspunktForNullstilling),
          () ->
              assertThat(statusKontrollereFar.getMor().getFoedselsnummer())
                  .isEqualTo(MOR.getFoedselsnummer()));
    }

    @Test
    void skalSetteAntallForsoekTilEnVedFeilDersomTidspunktForNullstillingErNaadd() {

      // given
      var antallDagerTilNullstilling = 0;
      var tidspunktForNullstilling = LocalDateTime.now().plusDays(antallDagerTilNullstilling);
      lagreStatusKontrollereFarMedMor(
          farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek() - 1,
          tidspunktForNullstilling);

      // when
      var statusKontrollereFar =
          persistenceService.oppdatereStatusKontrollereFar(
              MOR.getFoedselsnummer(),
              NAVN_FAR.sammensattNavn(),
              "Ronald McDonald",
              antallDagerTilNullstilling,
              farskapsportalFellesEgenskaper.getKontrollFarMaksAntallForsoek());

      // then
      var tidspunktEtterLogging = LocalDateTime.now();

      assertAll(
          () -> assertThat(statusKontrollereFar.getAntallFeiledeForsoek()).isEqualTo(1),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isAfter(tidspunktForNullstilling),
          () ->
              assertThat(statusKontrollereFar.getTidspunktForNullstilling())
                  .isBefore(tidspunktEtterLogging),
          () ->
              assertThat(statusKontrollereFar.getMor().getFoedselsnummer())
                  .isEqualTo(MOR.getFoedselsnummer()));
    }

    private StatusKontrollereFar lagreStatusKontrollereFarMedMor(
        int antallFeil, LocalDateTime tidspunktForNullstilling) {
      return statusKontrollereFarDao.save(
          StatusKontrollereFar.builder()
              .mor(henteForelder(Forelderrolle.MOR))
              .antallFeiledeForsoek(antallFeil)
              .tidspunktForNullstilling(tidspunktForNullstilling)
              .build());
    }
  }

  @Nested
  @DisplayName("IngenKonfliktMedEksisterendeFarskapserklaeringer")
  class IngenKonfliktMedEksisterendeFarskapserklaeringer {

    @BeforeEach
    void ryddeTestdata() {
      statusKontrollereFarDao.deleteAll();
      farskapserklaeringDao.deleteAll();
    }

    @Test
    void
        skalKasteValideringExceptionDersomMorHarEksisterendeFarskapserklaeringOgOppretterNyMedTermindato() {

      // given
      persistenceService.lagreNyFarskapserklaering(henteFarskapserklaering(MOR, FAR, UFOEDT_BARN));

      // when, then
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(
                      MOR.getFoedselsnummer(),
                      FAR.getFoedselsnummer(),
                      modelMapper.map(UFOEDT_BARN, BarnDto.class)));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.ERKLAERING_EKSISTERER_MOR);
    }

    @Test
    void skalKasteValideringExceptionDersomNyfoedtBarnInngaarIEksisterendeFarskapserklaering() {

      // given
      var fnrMorUtenEksisterendeFarskapserklaering =
          LocalDate.now().minusYears(29).format(DateTimeFormatter.ofPattern("ddMMyy")) + "12245";

      var dokument =
          Dokument.builder()
              .navn("farskapserklaering.pdf")
              .signeringsinformasjonMor(
                  Signeringsinformasjon.builder()
                      .redirectUrl(lageUrl(wiremockPort, "redirect-mor"))
                      .build())
              .signeringsinformasjonFar(
                  Signeringsinformasjon.builder()
                      .redirectUrl(lageUrl(wiremockPort, "/redirect-far"))
                      .build())
              .build();

      var farskapserklaering =
          Farskapserklaering.builder()
              .barn(NYFOEDT_BARN)
              .mor(MOR)
              .far(FAR)
              .dokument(dokument)
              .build();
      persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(
                      fnrMorUtenEksisterendeFarskapserklaering,
                      FAR.getFoedselsnummer(),
                      modelMapper.map(NYFOEDT_BARN, BarnDto.class)));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.ERKLAERING_EKSISTERER_BARN);
    }

    @Test
    void skalIkkeKasteExceptionDersomMorIkkeHarEksisterendeFarskapserklaering() {

      // given, when, then
      assertDoesNotThrow(
          () ->
              persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(
                  MOR.getFoedselsnummer(),
                  FAR.getFoedselsnummer(),
                  modelMapper.map(UFOEDT_BARN, BarnDto.class)));
    }

    @Test
    void skalIkkeKasteExceptionDersomMorHarEnEksisterendeDeaktivertFarskapserklaering() {

      // given
      UFOEDT_BARN.setId(0);
      var deaktivertFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(
              henteFarskapserklaering(MOR, FAR, UFOEDT_BARN));
      deaktivertFarskapserklaering.setDeaktivert(LocalDateTime.now());
      persistenceService.oppdatereFarskapserklaering(deaktivertFarskapserklaering);

      // when, then
      var ressursIkkeFunnetException =
          assertThrows(
              RessursIkkeFunnetException.class,
              () ->
                  persistenceService.henteFarskapserklaeringForId(
                      deaktivertFarskapserklaering.getId()));

      AssertionsForClassTypes.assertThat(ressursIkkeFunnetException.getFeilkode())
          .isEqualTo(Feilkode.FANT_IKKE_FARSKAPSERKLAERING);

      assertDoesNotThrow(
          () ->
              persistenceService.ingenKonfliktMedEksisterendeFarskapserklaeringer(
                  MOR.getFoedselsnummer(),
                  FAR.getFoedselsnummer(),
                  modelMapper.map(UFOEDT_BARN, BarnDto.class)));
    }
  }

  private Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "redirect-mor"))
                    .signeringstidspunkt(LocalDateTime.now())
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "/redirect-far"))
                    .build())
            .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }
}
