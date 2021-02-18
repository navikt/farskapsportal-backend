package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.FOEDSELSDATO_FAR;
import static no.nav.farskapsportal.TestUtils.FOEDSELSDATO_MOR;
import static no.nav.farskapsportal.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static no.nav.farskapsportal.TestUtils.henteNyligFoedtBarn;
import static no.nav.farskapsportal.TestUtils.lageUrl;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppretteFarskaperklaeringRequest;
import no.nav.farskapsportal.api.Sivilstandtype;
import no.nav.farskapsportal.config.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.KjoennDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.api.SivilstandDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.DokumentStatusDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.SignaturDto;
import no.nav.farskapsportal.exception.ManglerRelasjonException;
import no.nav.farskapsportal.exception.MorHarIngenNyfoedteUtenFarException;
import no.nav.farskapsportal.exception.NyfoedtErForGammelException;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("FarskapserklaeringService")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class FarskapsportalServiceTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto BARN = henteBarnUtenFnr(5);

  @MockBean
  PdfGeneratorConsumer pdfGeneratorConsumer;
  @MockBean
  DifiESignaturConsumer difiESignaturConsumer;
  @MockBean
  PersonopplysningService personopplysningService;
  @Autowired
  private PersistenceService persistenceService;
  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;
  @Autowired
  private FarskapsportalService farskapsportalService;
  @Autowired
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  @Nested
  @DisplayName("Teste henteBrukerinformasjon")
  class HenteBrukerinformasjon {

    @Test
    @DisplayName("Mor skal se sine påbegynte og fars ventende farskapserklæringer, og liste over nyfødte uten far")
    void morSkalSeSinePaabegynteOgFarsVentedeFarskapserklaeringerOgListeOverNyfoedteUtenFar() {

      // given
      farskapserklaeringDao.deleteAll();
      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(21);
      var spedbarnUtenFar = BarnDto.builder().foedselsnummer(foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "10100").build();
      var farskapserklaeringSomManglerSignaturFraMor = henteFarskapserklaering(MOR, FAR, spedbarnUtenFar);
      farskapserklaeringSomManglerSignaturFraMor.getDokument().setPadesUrl(null);

      var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(MOR, FAR, BARN);
      farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setPadesUrl(lageUrl("padesOppdatertVedSigneringMor"));
      farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(3));

      assertAll(() -> assertNull(farskapserklaeringSomManglerSignaturFraMor.getDokument().getSignertAvMor()),
          () -> assertNull(farskapserklaeringSomManglerSignaturFraMor.getDokument().getPadesUrl()),
          () -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvMor()),
          () -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvFar()));

      persistenceService.lagreFarskapserklaering(farskapserklaeringSomManglerSignaturFraMor);
      persistenceService.lagreFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(MOR.getForelderrolle());
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);

      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(spedbarnUtenFar.getFoedselsnummer()));

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      // then
      assertAll(() -> assertEquals(brukerinformasjon.getAvventerSigneringBruker().size(), 1),
          () -> assertEquals(brukerinformasjon.getFnrNyligFoedteBarnUtenRegistrertFar().size(), 1),
          () -> assertEquals(brukerinformasjon.getAvventerSigneringMotpart().size(), 1));
    }

    @Test
    @DisplayName("Mor skal se sine påbegynte farskapserklæringer")
    void morSkalSeSinePaabegynteFarskapserklaeringer() {

      // given
      farskapserklaeringDao.deleteAll();
      var farskapserklaeringSomManglerMorsSignatur = henteFarskapserklaering(MOR, FAR, BARN);
      farskapserklaeringSomManglerMorsSignatur.getDokument().setPadesUrl(null);

      assertAll(() -> assertNull(farskapserklaeringSomManglerMorsSignatur.getDokument().getSignertAvMor()),
          () -> assertNull(farskapserklaeringSomManglerMorsSignatur.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaeringSomManglerMorsSignatur.getDokument().getSignertAvFar()));

      persistenceService.lagreFarskapserklaering(farskapserklaeringSomManglerMorsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(MOR.getForelderrolle());
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      // then
      assertAll(() -> assertEquals(brukerinformasjon.getAvventerSigneringBruker().size(), 1),
          () -> assertEquals(brukerinformasjon.getFnrNyligFoedteBarnUtenRegistrertFar().size(), 0),
          () -> assertEquals(brukerinformasjon.getAvventerSigneringMotpart().size(), 0));
    }

    @Test
    @DisplayName("Mor skal se farskapserklæringer som venter på far")
    void morSkalSeFarskapserklaeringerSomVenterPaaFar() {

      // given
      farskapserklaeringDao.deleteAll();
      var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(MOR, FAR, BARN);
      farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

      assertAll(() -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvMor()),
          () -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvFar()));

      persistenceService.lagreFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(MOR.getForelderrolle());
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      // then
      assertAll(() -> assertEquals(brukerinformasjon.getAvventerSigneringMotpart().size(), 1),
          () -> assertTrue(brukerinformasjon.isKanOppretteFarskapserklaering()), () -> assertTrue(brukerinformasjon.isGyldigForelderrolle()),
          () -> assertTrue(brukerinformasjon.getFeilkodeTilgang().isEmpty()));
    }

    @Test
    @DisplayName("Dersom mor er gift skal feilkodeTilgang settes til feilkode for gift")
    void dersomMorErGiftSkalFeilkodeTilgangSettesTilFeilkodeForGift() {

      // given
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(MOR, FAR, BARN);
      farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

      assertAll(() -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvMor()),
          () -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvFar()));

      persistenceService.lagreFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(MOR.getForelderrolle());
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.GIFT).build());
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      // then
      assertAll(() -> assertEquals(brukerinformasjon.getAvventerSigneringMotpart().size(), 1),
          () -> assertFalse(brukerinformasjon.isKanOppretteFarskapserklaering()),
          () -> assertEquals(Feilkode.MOR_SIVILSTAND_GIFT, brukerinformasjon.getFeilkodeTilgang().get()));
    }


    @Test
    @DisplayName("Dersom mor har ukjent sivilstand skal feilkodeTilgang settes")
    void dersomMorHarUkjentSivilstandSkalFeilkodeTilgangSettes() {

      // given
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(MOR, FAR, BARN);
      farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

      assertAll(() -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvMor()),
          () -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvFar()));

      persistenceService.lagreFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(MOR.getForelderrolle());
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UOPPGITT).build());
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      // then
      assertAll(() -> assertEquals(brukerinformasjon.getAvventerSigneringMotpart().size(), 1),
          () -> assertFalse(brukerinformasjon.isKanOppretteFarskapserklaering()),
          () -> assertEquals(Feilkode.MOR_SIVILSTAND_UOPPGITT, brukerinformasjon.getFeilkodeTilgang().get()));
    }

    @Test
    @DisplayName("Dersom mor er registrert partner, skal feilkodeTilngang settes")
    void dersomMorErRegistrertPartnerSkalFeilkodeTilangSettes() {

      // given
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(MOR, FAR, BARN);
      farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

      assertAll(() -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvMor()),
          () -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvFar()));

      persistenceService.lagreFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(MOR.getForelderrolle());
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.REGISTRERT_PARTNER).build());
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      // then
      assertAll(() -> assertEquals(brukerinformasjon.getAvventerSigneringMotpart().size(), 1),
          () -> assertFalse(brukerinformasjon.isKanOppretteFarskapserklaering()),
          () -> assertEquals(Feilkode.MOR_SIVILSTAND_REGISTRERT_PARTNER, brukerinformasjon.getFeilkodeTilgang().get()));
    }

    @Test
    @DisplayName("Far skal se sine ventende farskapserklæringer")
    void farSkalSeSineVentendeFarskapserklaeringer() {

      // given
      farskapserklaeringDao.deleteAll();
      var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(MOR, FAR, BARN);
      farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

      assertAll(() -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvMor()),
          () -> assertNotNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvFar()));

      persistenceService.lagreFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer())).thenReturn(FAR.getForelderrolle());
      when(personopplysningService.henteSivilstand(FAR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(FAR.getFoedselsnummer());

      // then
      assertEquals(1, brukerinformasjon.getAvventerSigneringBruker().size());
    }

    @Test
    @DisplayName("Far skal ikke se farskapserklæringer som mor ikke har signert")
    void farSkalIkkeSeFarskapserklaeringerSomMorIkkeHarSignert() {

      // given
      farskapserklaeringDao.deleteAll();
      var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(MOR, FAR, BARN);
      farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setPadesUrl(null);

      assertAll(() -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvMor()),
          () -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSignertAvFar()));

      persistenceService.lagreFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer())).thenReturn(FAR.getForelderrolle());
      when(personopplysningService.henteSivilstand(FAR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(FAR.getFoedselsnummer());

      // then
      assertAll(() -> assertEquals(0, brukerinformasjon.getAvventerSigneringBruker().size()),
          () -> assertEquals(0, brukerinformasjon.getAvventerSigneringMotpart().size()),
          () -> assertEquals(0, brukerinformasjon.getFnrNyligFoedteBarnUtenRegistrertFar().size()));
    }
  }

  @Nested
  @DisplayName("Teste oppretteFarskapserklaering")
  class OppretteFarskapserklaering {

    @SneakyThrows
    @Test
    @DisplayName("Skal opprette farskapserklæring for barn med termindato")
    void skalOppretteFarskapserklaeringForBarnMedTermindato() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var barn = henteBarnUtenFnr(4);
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var registrertNavnFar = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
      var opplysningerOmFar = KontrollerePersonopplysningerRequest.builder().foedselsnummer(FAR.getFoedselsnummer())
          .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn()).build();

      var pdf = DokumentDto.builder().dokumentnavn("Farskapserklæering.pdf").innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
          .redirectUrlMor(lageUrl("redirect-mor")).build();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(registrertNavnMor);
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(Forelderrolle.MOR);

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(registrertNavnFar);
      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);

      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);
      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when
      var respons = farskapsportalService.oppretteFarskapserklaering(MOR.getFoedselsnummer(),
          OppretteFarskaperklaeringRequest.builder().barn(barn).opplysningerOmFar(opplysningerOmFar).build());

      // then
      assertEquals(pdf.getRedirectUrlMor(), respons.getRedirectUrlForSigneringMor());
    }

    @Test
    @DisplayName("Skal opprette farskapserklæring for nyfødt")
    void skalOppretteFarskapserklaeringForNyfoedt() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var foedselsdatoBarn = LocalDate.now().minusMonths(farskapsportalEgenskaper.getMaksAntallMaanederEtterFoedsel()).plusDays(1);
      var barnFoedtInnenforGyldigIntervall = henteBarnMedFnr(foedselsdatoBarn);
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var registrertNavnFar = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
      var opplysningerOmFar = KontrollerePersonopplysningerRequest.builder().foedselsnummer(FAR.getFoedselsnummer())
          .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn()).build();

      var pdf = DokumentDto.builder().dokumentnavn("Farskapserklæering.pdf").innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
          .redirectUrlMor(lageUrl("redirect-mor")).build();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(registrertNavnMor);
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(barnFoedtInnenforGyldigIntervall.getFoedselsnummer()));
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(Forelderrolle.MOR);

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(registrertNavnFar);
      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);

      when(personopplysningService.henteFoedselsdato(barnFoedtInnenforGyldigIntervall.getFoedselsnummer())).thenReturn(foedselsdatoBarn);
      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);
      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when
      var respons = farskapsportalService.oppretteFarskapserklaering(MOR.getFoedselsnummer(),
          OppretteFarskaperklaeringRequest.builder().barn(barnFoedtInnenforGyldigIntervall).opplysningerOmFar(opplysningerOmFar).build());

      // then
      assertEquals(pdf.getRedirectUrlMor(), respons.getRedirectUrlForSigneringMor());
    }

    @Test
    @DisplayName("Skal kaste IllegalArgumentException dersom mor og far er samme person")
    void skalKasteIllegalArgumentExceptionDersomMorOgFarErSammePerson() {

      // given
      var barn = henteBarnUtenFnr(4);
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var registrertNavnFar = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var opplysningerOmFar = KontrollerePersonopplysningerRequest.builder().foedselsnummer(MOR.getFoedselsnummer())
          .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn()).build();

      var pdf = DokumentDto.builder().dokumentnavn("Farskapserklæering.pdf").innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
          .redirectUrlMor(lageUrl("redirect-mor")).build();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(registrertNavnMor);
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(Forelderrolle.MOR);

      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);
      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when, then
      assertThrows(IllegalArgumentException.class, () -> farskapsportalService.oppretteFarskapserklaering(MOR.getFoedselsnummer(),
          OppretteFarskaperklaeringRequest.builder().barn(barn).opplysningerOmFar(opplysningerOmFar).build()));
    }

    @Test
    @DisplayName("Skal kaste IllegalArgumentExceptionDersomTermindatoErUgyldig")
    void skalKasteIllegalArgumentExceptionDersomTermindatoErUgyldig() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      //given
      var barnMedTermindatoForLangtFremITid = henteBarnUtenFnr(farskapsportalEgenskaper.getMaksAntallUkerTilTermindato() + 2);
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var registrertNavnFar = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
      var opplysningerOmFar = KontrollerePersonopplysningerRequest.builder().foedselsnummer(FAR.getFoedselsnummer())
          .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn()).build();

      var pdf = DokumentDto.builder().dokumentnavn("Farskapserklæering.pdf").innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
          .redirectUrlMor(lageUrl("redirect-mor")).build();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(registrertNavnMor);
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(Forelderrolle.MOR);

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(registrertNavnFar);
      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);
      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when, then
      assertThrows(IllegalArgumentException.class, () -> farskapsportalService.oppretteFarskapserklaering(MOR.getFoedselsnummer(),
          OppretteFarskaperklaeringRequest.builder().barn(barnMedTermindatoForLangtFremITid).opplysningerOmFar(opplysningerOmFar).build()));
    }

    @Test
    @DisplayName("Skal kaste ManglerRelasjonException dersom barn oppgitt med fødselsnummer mangler relasjon til mor")
    void skalKasteManglerRelasjonExceptionDersomBarnOppgittMedFoedselsnummerManglerRelasjonTilMor() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var fnrSpedbarnUtenFar = LocalDate.now().minusMonths(2).minusDays(-5).format(DateTimeFormatter.ofPattern("ddMMyy")) + "13333";
      var barnUtenRelasjonTilMor = BarnDto.builder()
          .foedselsnummer(LocalDate.now().minusMonths(2).minusDays(21).format(DateTimeFormatter.ofPattern("ddMMyy")) + "10100").build();
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var registrertNavnFar = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
      var opplysningerOmFar = KontrollerePersonopplysningerRequest.builder().foedselsnummer(FAR.getFoedselsnummer())
          .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn()).build();

      var pdf = DokumentDto.builder().dokumentnavn("Farskapserklæering.pdf").innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
          .redirectUrlMor(lageUrl("redirect-mor")).build();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(registrertNavnMor);
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer())).thenReturn(new HashSet<>());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(registrertNavnFar);
      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer())).thenReturn(Set.of(fnrSpedbarnUtenFar));

      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when, then
      assertThrows(ManglerRelasjonException.class, () -> farskapsportalService.oppretteFarskapserklaering(MOR.getFoedselsnummer(),
          OppretteFarskaperklaeringRequest.builder().barn(barnUtenRelasjonTilMor).opplysningerOmFar(opplysningerOmFar).build()));

    }

    @Test
    @DisplayName("Skal kaste MorHarIngenNyfoedteUtenFar exception dersom mor ikke er registrert med nyfødte ban uten far")
    void skalKasteMorHarIngenNyfoedteUtenFarException() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var nyfoedt = henteNyligFoedtBarn();
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var registrertNavnFar = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
      var opplysningerOmFar = KontrollerePersonopplysningerRequest.builder().foedselsnummer(FAR.getFoedselsnummer())
          .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn()).build();

      var pdf = DokumentDto.builder().dokumentnavn("Farskapserklæering.pdf").innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
          .redirectUrlMor(lageUrl("redirect-mor")).build();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(registrertNavnMor);
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer())).thenReturn(new HashSet<>());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(registrertNavnFar);
      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);

      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when, then
      assertThrows(MorHarIngenNyfoedteUtenFarException.class, () -> farskapsportalService.oppretteFarskapserklaering(MOR.getFoedselsnummer(),
          OppretteFarskaperklaeringRequest.builder().barn(nyfoedt).opplysningerOmFar(opplysningerOmFar).build()));
    }

    @Test
    @DisplayName("Skal kaste NyfoedtErForGammelException dersom nyfødt er for gammel")
    void skalKasteNyfoedtErForGammelExceptionDersomNyfoedtErForGammel() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var foedselsdatoNyfoedt = LocalDate.now().minusMonths(farskapsportalEgenskaper.getMaksAntallMaanederEtterFoedsel());
      var nyfoedt = henteBarnMedFnr(foedselsdatoNyfoedt);
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var registrertNavnFar = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
      var opplysningerOmFar = KontrollerePersonopplysningerRequest.builder().foedselsnummer(FAR.getFoedselsnummer())
          .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn()).build();

      var pdf = DokumentDto.builder().dokumentnavn("Farskapserklæering.pdf").innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
          .redirectUrlMor(lageUrl("redirect-mor")).build();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(registrertNavnMor);
      when(personopplysningService.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer())).thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer())).thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer())).thenReturn(Set.of(nyfoedt.getFoedselsnummer()));

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(registrertNavnFar);
      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);

      when(personopplysningService.henteFoedselsdato(nyfoedt.getFoedselsnummer())).thenReturn(foedselsdatoNyfoedt);
      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);

      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when, then
      assertThrows(NyfoedtErForGammelException.class, () -> farskapsportalService.oppretteFarskapserklaering(MOR.getFoedselsnummer(),
          OppretteFarskaperklaeringRequest.builder().barn(nyfoedt).opplysningerOmFar(opplysningerOmFar).build()));

    }
  }

  @Nested
  @DisplayName("Teste henteSignertDokumentEtterRedirect")
  class HenteSignertDokumentEtterRedirect {

    @Test
    @DisplayName("Far skal se dokument etter redirect dersom status query token er gyldig")
    void farSkalSeDokumentEtterRedirectDersomStatusQueryTokenErGyldig() {

      // given
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUrl("status");
      var farskapserklaering = henteFarskapserklaering(MOR, FAR, BARN);
      var padesFar = lageUrl("padesFar");
      farskapserklaering.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(3));

      assertAll(() -> assertNotNull(farskapserklaering.getDokument().getPadesUrl()),
          () -> assertNull(farskapserklaering.getDokument().getSignertAvFar()));

      var lagretFarskapserklaering = persistenceService.lagreFarskapserklaering(farskapserklaering);

      when(personopplysningService.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer())).thenReturn(FAR.getForelderrolle());
      when(personopplysningService.henteGjeldendeKjoenn(FAR.getFoedselsnummer())).thenReturn(KjoennDto.builder().kjoenn(KjoennType.MANN).build());

      when(difiESignaturConsumer.henteDokumentstatusEtterRedirect(any(), any())).thenReturn(
          DokumentStatusDto.builder().statuslenke(statuslenke).erSigneringsjobbenFerdig(true).padeslenke(padesFar).signaturer(List.of(
              SignaturDto.builder().signatureier(FAR.getFoedselsnummer()).harSignert(true).tidspunktForSignering(LocalDateTime.now().minusSeconds(3))
                  .build())).build());

      when(difiESignaturConsumer.henteSignertDokument(any())).thenReturn(farskapserklaering.getDokument().getInnhold());

      // when
      var respons = farskapsportalService.henteSignertDokumentEtterRedirect(FAR.getFoedselsnummer(), "etGyldigStatusQueryToken");

      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId()).get();

      // then
      assertAll(() -> assertNotNull(oppdatertFarskapserklaering.getDokument().getSignertAvFar()),
          () -> assertEquals(padesFar.toString(), oppdatertFarskapserklaering.getDokument().getPadesUrl()),
          () -> assertEquals(farskapserklaering.getDokument().getInnhold(), respons));
    }
  }
}
