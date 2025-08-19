package no.nav.farskapsportal.backend.apps.api.service;

import static no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK;
import static no.nav.farskapsportal.backend.apps.api.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_NYFOEDT_BARN;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.NAVN_FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.NAVN_MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteNyligFoedtBarn;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUri;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.tilUri;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiTestConfig;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalApiEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.backend.apps.api.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.backend.apps.api.model.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.backend.apps.api.model.OppdatereFarskapserklaeringRequest;
import no.nav.farskapsportal.backend.apps.api.model.OppretteFarskapserklaeringRequest;
import no.nav.farskapsportal.backend.apps.api.model.StatusSignering;
import no.nav.farskapsportal.backend.libs.dto.BarnDto;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.Rolle;
import no.nav.farskapsportal.backend.libs.dto.pdl.FolkeregisteridentifikatorDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonRolle.Sivilstandtype;
import no.nav.farskapsportal.backend.libs.dto.pdl.KjoennDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.KjoennType;
import no.nav.farskapsportal.backend.libs.dto.pdl.SivilstandDto;
import no.nav.farskapsportal.backend.libs.entity.*;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.BucketConsumer;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.exception.EsigneringStatusFeiletException;
import no.nav.farskapsportal.backend.libs.felles.exception.FeilNavnOppgittException;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.exception.PersonIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.exception.RessursIkkeFunnetException;
import no.nav.farskapsportal.backend.libs.felles.exception.ValideringException;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("FarskapserklaeringService")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {FarskapsportalApiApplicationLocal.class, FarskapsportalApiTestConfig.class})
@AutoConfigureWireMock(port = 0)
@EnableMockOAuth2Server
@ActiveProfiles(PROFILE_TEST)
public class FarskapsportalServiceTest {

  @Value("${wiremock.server.port}")
  String wiremockPort;

  private @MockBean PdfGeneratorConsumer pdfGeneratorConsumer;
  private @MockBean DifiESignaturConsumer difiESignaturConsumer;
  private @MockBean PersonopplysningService personopplysningService;
  private @MockBean BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private @MockBean BucketConsumer bucketConsumer;
  private @MockBean GcpStorageManager gcpStorageManager;
  private @Autowired PersistenceService persistenceService;
  private @Autowired OppgavebestillingDao oppgavebestillingDao;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @Autowired ForelderDao forelderDao;
  private @Autowired StatusKontrollereFarDao statusKontrollereFarDao;
  private @Autowired FarskapsportalApiEgenskaper farskapsportalApiEgenskaper;
  private @Autowired Mapper mapper;

  private FarskapsportalService farskapsportalService;

  @BeforeEach
  public void setup() {
    farskapsportalService =
        FarskapsportalService.builder()
            .farskapsportalApiEgenskaper(farskapsportalApiEgenskaper)
            .pdfGeneratorConsumer(pdfGeneratorConsumer)
            .difiESignaturConsumer(difiESignaturConsumer)
            .persistenceService(persistenceService)
            .persistenceService(persistenceService)
            .personopplysningService(personopplysningService)
            .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
            .bucketConsumer(bucketConsumer)
            .mapper(mapper)
            .build();
  }

  private Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument =
        Dokument.builder()
            .padesUrl("https://pades.url")
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

  @Nested
  @DisplayName("Teste henteBrukerinformasjon")
  class HenteBrukerinformasjon {

    @Test
    @DisplayName("Mor skal se liste over nyfødte uten far")
    void morSkalSeSinePaabegynteOgFarsVentedeFarskapserklaeringerOgListeOverNyfoedteUtenFar() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(21);
      var spedbarnUtenFar =
          BarnDto.builder()
              .foedselsnummer(
                  foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "10100")
              .build();

      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      var farskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);
      persistenceService.oppdatereFarskapserklaering(farskapserklaering);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);

      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(spedbarnUtenFar.getFoedselsnummer()));

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      assertAll(
          () ->
              Assertions.assertEquals(
                  1, brukerinformasjon.getFnrNyligFoedteBarnUtenRegistrertFar().size()),
          () ->
              Assertions.assertEquals(
                  1,
                  brukerinformasjon.getAvventerSigneringMotpart().stream()
                      .filter(fe -> fe.getPaaloggetBrukersRolle().equals(Rolle.MOR))
                      .collect(Collectors.toSet())
                      .size()));
    }

    @Test
    @DisplayName("Mor skal se sine påbegynte farskapserklæringer")
    void morSkalSeSinePaabegynteFarskapserklaeringer() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomManglerMorsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));

      farskapserklaeringSomManglerMorsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(null);

      assertAll(
          () ->
              assertNull(
                  farskapserklaeringSomManglerMorsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomManglerMorsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      var farskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerMorsSignatur);
      persistenceService.oppdatereFarskapserklaering(farskapserklaering);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      // then
      assertAll(
          () ->
              Assertions.assertEquals(
                  1,
                  brukerinformasjon.getAvventerSigneringBruker().stream()
                      .filter(fe -> fe.getPaaloggetBrukersRolle().equals(Rolle.MOR))
                      .collect(Collectors.toSet())
                      .size()),
          () ->
              Assertions.assertEquals(
                  0, brukerinformasjon.getFnrNyligFoedteBarnUtenRegistrertFar().size()),
          () -> Assertions.assertEquals(0, brukerinformasjon.getAvventerSigneringMotpart().size()));
    }

    @Test
    @DisplayName("Mor skal se farskapserklæringer som venter på far")
    void morSkalSeFarskapserklaeringerSomVenterPaaFar() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      var farskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);
      persistenceService.oppdatereFarskapserklaering(farskapserklaering);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      Assertions.assertEquals(
          1,
          brukerinformasjon.getAvventerSigneringMotpart().stream()
              .filter(fe -> fe.getPaaloggetBrukersRolle().equals(Rolle.MOR))
              .collect(Collectors.toSet())
              .size());
    }

    @Test
    void morSkalSeSineNyfoedteBarnSomIkkeHarRegistrertFar() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var barnFoedtInnenforGyldigIntervall =
          henteBarnMedFnr(
              LocalDate.now()
                  .minusMonths(
                      farskapsportalApiEgenskaper
                              .getFarskapsportalFellesEgenskaper()
                              .getMaksAntallMaanederEtterFoedsel()
                          + 1));
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(barnFoedtInnenforGyldigIntervall.getFoedselsnummer()));
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer());

      Assertions.assertEquals(
          1,
          brukerinformasjon.getFnrNyligFoedteBarnUtenRegistrertFar().stream()
              .filter(
                  fnrNyfoedt ->
                      fnrNyfoedt.equals(barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
              .collect(Collectors.toSet())
              .size());
    }

    @Test
    @DisplayName("Skal ikke kaste ValideringException dersom mor er separert")
    void skalIkkeKasteValideringExceptionDersomMorErSeparert() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.SEPARERT).build());
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);

      // when
      var response =
          assertDoesNotThrow(
              () -> farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer()));

      assertThat(response.isKanOppretteFarskapserklaering());
    }

    @Test
    @DisplayName("Skal kaste ValideringException dersom mor er gift")
    void skalKasteValideringExceptionDersomMorErGift() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.GIFT).build());
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer()));

      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.MOR_SIVILSTAND_GIFT);
    }

    @Test
    @DisplayName("Skal kaste ValideringException dersom mor har ukjent sivilstand")
    void skalKasteValideringExceptionDersomMorHarUkjentSivilstand() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UOPPGITT).build());
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.MOR_SIVILSTAND_UOPPGITT);
    }

    @Test
    @DisplayName("Skal kaste ValideringException dersom mor er registrert partner")
    void skalKasteValideringExceptiondersomMorErRegistrertPartner() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.REGISTRERT_PARTNER).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer()));

      assertThat(valideringException.getFeilkode())
          .isEqualTo(Feilkode.MOR_SIVILSTAND_REGISTRERT_PARTNER);
    }

    @Test
    void skalKasteValideringExceptionDersomMorErBosattUtenforNorge() {

      // given
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(false);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(FolkeregisteridentifikatorDto.builder().type("FNR").status("I_BRUK").build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.henteBrukerinformasjon(MOR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode())
          .isEqualTo(Feilkode.MOR_IKKE_NORSK_BOSTEDSADRESSE);
    }

    @Test
    @DisplayName("Far skal se sine ventende farskapserklæringer")
    void farSkalSeSineVentendeFarskapserklaeringer() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      var farskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);
      persistenceService.oppdatereFarskapserklaering(farskapserklaering);

      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteSivilstand(FAR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harVerge(MOR.getFoedselsnummer())).thenReturn(false);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(FAR.getFoedselsnummer());

      // then
      Assertions.assertEquals(
          1,
          brukerinformasjon.getAvventerSigneringBruker().stream()
              .filter(fe -> fe.getPaaloggetBrukersRolle().equals(Rolle.FAR))
              .collect(Collectors.toSet())
              .size());
    }

    @Test
    @DisplayName("Far skal ikke se farskapserklæringer som mor ikke har signert")
    void farSkalIkkeSeFarskapserklaeringerSomMorIkkeHarSignert() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();
      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));

      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(null);

      assertAll(
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harVerge(FAR.getFoedselsnummer())).thenReturn(false);
      when(personopplysningService.henteSivilstand(FAR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(FAR.getFoedselsnummer());

      // then
      assertAll(
          () -> Assertions.assertEquals(0, brukerinformasjon.getAvventerSigneringBruker().size()),
          () -> Assertions.assertEquals(0, brukerinformasjon.getAvventerSigneringMotpart().size()),
          () ->
              Assertions.assertEquals(
                  0, brukerinformasjon.getFnrNyligFoedteBarnUtenRegistrertFar().size()));
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
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var barn = henteBarnUtenFnr(4);
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES))
              .name("fp-1.pdf")
              .build();
      when(bucketConsumer.lagrePades(1, pdf)).thenReturn(blobIdGcp);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);

      // legger på redirecturl til dokument i void-metode
      doAnswer(
              invocation -> {
                Object[] args = invocation.getArguments();
                var dokument = (Dokument) args[1];
                dokument
                    .getSigneringsinformasjonMor()
                    .setRedirectUrl(lageUrl(wiremockPort, "/mors-redirect"));
                return null;
              })
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      farskapsportalService.oppretteFarskapserklaering(
          MOR.getFoedselsnummer(),
          OppretteFarskapserklaeringRequest.builder()
              .barn(mapper.toDto(barn))
              .opplysningerOmFar(opplysningerOmFar)
              .build());

      // then
      var opprettetFarskapserklaering =
          persistenceService.henteFarskapserklaeringerForForelder(MOR.getFoedselsnummer());

      assertAll(
          () -> assertThat(opprettetFarskapserklaering.size()).isEqualTo(1),
          () ->
              assertThat(
                      opprettetFarskapserklaering.stream().findAny().get().getFarBorSammenMedMor())
                  .isNull(),
          () ->
              assertThat(
                      opprettetFarskapserklaering.stream()
                          .findAny()
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getSendtTilSignering())
                  .isNotNull(),
          () ->
              assertThat(
                      opprettetFarskapserklaering.stream()
                          .findAny()
                          .get()
                          .getFar()
                          .getFoedselsnummer())
                  .isEqualTo(FAR.getFoedselsnummer()));
    }

    @Test
    @DisplayName("Skal opprette farskapserklæring for nyfødt")
    void skalOppretteFarskapserklaeringForNyligFoedtBarnFoedtINorge() {

      // rydde testdata
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var foedselsdatoBarn =
          LocalDate.now()
              .minusMonths(
                  farskapsportalApiEgenskaper
                      .getFarskapsportalFellesEgenskaper()
                      .getMaksAntallMaanederEtterFoedsel())
              .plusDays(1);
      var barnFoedtInnenforGyldigIntervall = henteBarnMedFnr(foedselsdatoBarn);
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES))
              .name("fp-1.pdf")
              .build();
      when(bucketConsumer.lagrePades(1, pdf)).thenReturn(blobIdGcp);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(barnFoedtInnenforGyldigIntervall.getFoedselsnummer()));
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteFødeland(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn(FarskapsportalService.KODE_LAND_NORGE);
      when(personopplysningService.henteFødselsdato(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn(foedselsdatoBarn);
      when(personopplysningService.henteFolkeregisteridentifikator(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);

      // legger på redirecturl til dokument i void-metode
      doAnswer(
              invocation -> {
                Object[] args = invocation.getArguments();
                var dokument = (Dokument) args[1];
                dokument.setSigneringsinformasjonMor(
                    Signeringsinformasjon.builder()
                        .redirectUrl(lageUrl(wiremockPort, "/mors-redirect"))
                        .build());
                return null;
              })
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      farskapsportalService.oppretteFarskapserklaering(
          MOR.getFoedselsnummer(),
          OppretteFarskapserklaeringRequest.builder()
              .barn(mapper.toDto(barnFoedtInnenforGyldigIntervall))
              .opplysningerOmFar(opplysningerOmFar)
              .build());

      // then
      var farskapserklaeringer =
          farskapserklaeringDao.henteFarskapserklaeringerForForelder(MOR.getFoedselsnummer());

      assertThat(farskapserklaeringer.size()).isEqualTo(1);
    }

    @Test
    void skalKasteValideringExceptionDersomBarnErFoedtIUtlandet() {

      // rydde testdata
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var foedselsdatoBarn =
          LocalDate.now()
              .minusMonths(
                  farskapsportalApiEgenskaper
                      .getFarskapsportalFellesEgenskaper()
                      .getMaksAntallMaanederEtterFoedsel())
              .plusDays(1);
      var barnFoedtInnenforGyldigIntervall = mapper.toDto(henteBarnMedFnr(foedselsdatoBarn));
      barnFoedtInnenforGyldigIntervall.setFoedested("Slottsplassen");
      barnFoedtInnenforGyldigIntervall.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(barnFoedtInnenforGyldigIntervall.getFoedselsnummer()));
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());

      when(personopplysningService.henteFødeland(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn("Mexico");
      when(personopplysningService.henteFødselsdato(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn(foedselsdatoBarn);
      when(personopplysningService.henteFolkeregisteridentifikator(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());
      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);

      // legger på redirecturl til dokument i void-metode
      doAnswer(
              invocation -> {
                Object[] args = invocation.getArguments();
                var dokument = (Dokument) args[0];
                dokument.setSigneringsinformasjonMor(
                    Signeringsinformasjon.builder()
                        .redirectUrl(lageUrl(wiremockPort, "/mors-redirect"))
                        .build());
                return null;
              })
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when, then
      assertThrows(
          ValideringException.class,
          () ->
              farskapsportalService.oppretteFarskapserklaering(
                  MOR.getFoedselsnummer(),
                  OppretteFarskapserklaeringRequest.builder()
                      .barn(barnFoedtInnenforGyldigIntervall)
                      .opplysningerOmFar(opplysningerOmFar)
                      .build()));
    }

    @Test
    @DisplayName(
        "Mor skal kunne opprette farskapserklæring for nyfødt barn selv om hun har en pågående farskapserklæring for ufødt")
    void morSkalKunneOppretteFarskapserklaeringForNyfoedtSelvOmHunHarEnAapenErklaeringForUfoedt() {

      // rydde testdata
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var foedselsdatoBarn =
          LocalDate.now()
              .minusMonths(
                  farskapsportalApiEgenskaper
                      .getFarskapsportalFellesEgenskaper()
                      .getMaksAntallMaanederEtterFoedsel())
              .plusDays(1);
      var barnFoedtInnenforGyldigIntervall = mapper.toDto(henteBarnMedFnr(foedselsdatoBarn));
      barnFoedtInnenforGyldigIntervall.setFoedested("Fornebu");
      barnFoedtInnenforGyldigIntervall.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);
      var eksisterendeFarskapserklaeringUfoedtBarnVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();
      var redirectUrlMor = "https://esignering.no/redirect-mor";
      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      eksisterendeFarskapserklaeringUfoedtBarnVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());
      assertAll(
          () ->
              assertNotNull(
                  eksisterendeFarskapserklaeringUfoedtBarnVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  eksisterendeFarskapserklaeringUfoedtBarnVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES))
              .name("fp-1.pdf")
              .build();

      when(bucketConsumer.lagrePades(1, pdf)).thenReturn(blobIdGcp);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(barnFoedtInnenforGyldigIntervall.getFoedselsnummer()));
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.erDoed(FAR.getFoedselsnummer())).thenReturn(false);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteFødeland(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn(FarskapsportalService.KODE_LAND_NORGE);
      when(personopplysningService.henteFødselsdato(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn(foedselsdatoBarn);
      when(personopplysningService.henteFolkeregisteridentifikator(
              barnFoedtInnenforGyldigIntervall.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);

      // legger på redirecturl til dokument i void-metode
      doAnswer(
              invocation -> {
                Object[] args = invocation.getArguments();
                var dokument = (Dokument) args[1];
                dokument.setSigneringsinformasjonMor(
                    Signeringsinformasjon.builder().redirectUrl(redirectUrlMor).build());
                return dokument;
              })
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      var respons =
          farskapsportalService.oppretteFarskapserklaering(
              MOR.getFoedselsnummer(),
              OppretteFarskapserklaeringRequest.builder()
                  .barn(barnFoedtInnenforGyldigIntervall)
                  .opplysningerOmFar(opplysningerOmFar)
                  .build());

      // then
      assertThat(redirectUrlMor).isEqualTo(respons.getRedirectUrlForSigneringMor());
    }

    @Test
    @DisplayName(
        "Mor skal ikke kunne opprette farskapserklæring for ufødt barn dersom hun har en pågående farskapserklæring")
    void
        morSkalIkkeKunneOppretteFarskapserklaeringForUfoedtBarnDersomHunHarEnPaagaaendeFarskapserklaering() {

      // rydde testdata
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var ufoedtBarn = mapper.toDto(henteBarnUtenFnr(13));
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var farskapserklaeringSomVenterPaaFarsSignatur =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));

      farskapserklaeringSomVenterPaaFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);

      // when, then
      assertThrows(
          ValideringException.class,
          () ->
              farskapsportalService.oppretteFarskapserklaering(
                  MOR.getFoedselsnummer(),
                  OppretteFarskapserklaeringRequest.builder()
                      .barn(ufoedtBarn)
                      .opplysningerOmFar(opplysningerOmFar)
                      .build()));
    }

    @Test
    @DisplayName(
        "Skal kaste ValideringException dersom mor har åpen erklæring med annen far for nyfødte barn")
    void skalKasteValideringExceptionDersomMorHarAapenErklaeringMedAnnenFarForNyfoedteBarn() {

      // rydde testdata
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var enAnnenFar =
          Forelder.builder()
              .foedselsnummer(
                  LocalDate.now().minusYears(35).format(DateTimeFormatter.ofPattern("ddMMyy"))
                      + "01011")
              .build();

      var foedselsdatoNyfoedte = LocalDate.now().minusMonths(1);
      var nyfoedtBarn1 = henteBarnMedFnr(foedselsdatoNyfoedte);
      var nyfoedtBarn2 =
          BarnDto.builder()
              .foedselsnummer(
                  foedselsdatoNyfoedte.format(DateTimeFormatter.ofPattern("ddMMyy")) + "11111")
              .build();
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();
      var farskapserklaeringSomVenterPaaEnAnnenFarsSignatur =
          henteFarskapserklaering(MOR, enAnnenFar, nyfoedtBarn1);
      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      farskapserklaeringSomVenterPaaEnAnnenFarsSignatur
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());

      assertAll(
          () ->
              assertNotNull(
                  farskapserklaeringSomVenterPaaEnAnnenFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()),
          () ->
              assertNull(
                  farskapserklaeringSomVenterPaaEnAnnenFarsSignatur
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));

      persistenceService.lagreNyFarskapserklaering(
          farskapserklaeringSomVenterPaaEnAnnenFarsSignatur);

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(nyfoedtBarn2.getFoedselsnummer()));
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteFødselsdato(nyfoedtBarn2.getFoedselsnummer()))
          .thenReturn(foedselsdatoNyfoedte);
      when(personopplysningService.henteFødeland(nyfoedtBarn2.getFoedselsnummer()))
          .thenReturn(FarskapsportalService.KODE_LAND_NORGE);
      when(personopplysningService.henteFolkeregisteridentifikator(
              nyfoedtBarn2.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);
      doNothing()
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.oppretteFarskapserklaering(
                      MOR.getFoedselsnummer(),
                      OppretteFarskapserklaeringRequest.builder()
                          .barn(nyfoedtBarn2)
                          .opplysningerOmFar(opplysningerOmFar)
                          .build()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.FORSKJELLIGE_FEDRE);
    }

    @Test
    @DisplayName("Skal kaste ValideringFeiletException dersom mor og far er samme person")
    void skalKasteIllegalArgumentExceptionDersomMorOgFarErSammePerson() {

      // given
      var barn = mapper.toDto(henteBarnUtenFnr(4));
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_MOR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(MOR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());

      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);
      doNothing()
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.oppretteFarskapserklaering(
                      MOR.getFoedselsnummer(),
                      OppretteFarskapserklaeringRequest.builder()
                          .barn(barn)
                          .opplysningerOmFar(opplysningerOmFar)
                          .build()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.MOR_OG_FAR_SAMME_PERSON);
    }

    @Test
    @DisplayName("Skal kaste ValideringException dersom termindato er ugyldig")
    void skalKasteValideringExceptionDersomTermindatoErUgyldig() {

      // rydde testdata
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var barnMedTermindatoForLangtFremITid =
          mapper.toDto(
              henteBarnUtenFnr(farskapsportalApiEgenskaper.getMaksAntallUkerTilTermindato() + 2));
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);
      doNothing()
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.oppretteFarskapserklaering(
                      MOR.getFoedselsnummer(),
                      OppretteFarskapserklaeringRequest.builder()
                          .barn(barnMedTermindatoForLangtFremITid)
                          .opplysningerOmFar(opplysningerOmFar)
                          .build()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.TERMINDATO_UGYLDIG);
    }

    @Test
    @DisplayName(
        "Skal kaste ManglerRelasjonException dersom barn oppgitt med fødselsnummer mangler relasjon til mor")
    void
        skalKasteManglerRelasjonExceptionDersomBarnOppgittMedFoedselsnummerManglerRelasjonTilMor() {

      // rydde testdata
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var fnrSpedbarnUtenFar =
          LocalDate.now().minusMonths(2).minusDays(-5).format(DateTimeFormatter.ofPattern("ddMMyy"))
              + "13333";
      var foedselsdatoBarnUtenRelasjonTilMor = LocalDate.now().minusMonths(2).minusDays(21);
      var barnUtenRelasjonTilMor =
          BarnDto.builder()
              .foedselsnummer(
                  LocalDate.now()
                          .minusMonths(2)
                          .minusDays(21)
                          .format(DateTimeFormatter.ofPattern("ddMMyy"))
                      + "10100")
              .foedselsdato(foedselsdatoBarnUtenRelasjonTilMor)
              .build();
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(new HashSet<>());
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(fnrSpedbarnUtenFar));
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .identifikasjonsnummer(MOR.getFoedselsnummer())
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .identifikasjonsnummer(FAR.getFoedselsnummer())
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);

      when(personopplysningService.henteFødeland(fnrSpedbarnUtenFar))
          .thenReturn(FarskapsportalService.KODE_LAND_NORGE);
      when(personopplysningService.henteFødeland(barnUtenRelasjonTilMor.getFoedselsnummer()))
          .thenReturn(FarskapsportalService.KODE_LAND_NORGE);
      when(personopplysningService.henteFødselsdato(barnUtenRelasjonTilMor.getFoedselsnummer()))
          .thenReturn(foedselsdatoBarnUtenRelasjonTilMor);
      when(personopplysningService.henteFolkeregisteridentifikator(fnrSpedbarnUtenFar))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .identifikasjonsnummer(fnrSpedbarnUtenFar)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());
      when(personopplysningService.henteFolkeregisteridentifikator(
              barnUtenRelasjonTilMor.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .identifikasjonsnummer(barnUtenRelasjonTilMor.getFoedselsnummer())
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      doNothing()
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.oppretteFarskapserklaering(
                      MOR.getFoedselsnummer(),
                      OppretteFarskapserklaeringRequest.builder()
                          .barn(barnUtenRelasjonTilMor)
                          .opplysningerOmFar(opplysningerOmFar)
                          .build()));

      // then
      assertThat(valideringException.getFeilkode())
          .isEqualTo(Feilkode.BARN_MANGLER_RELASJON_TIL_MOR);
    }

    @Test
    @DisplayName(
        "Skal kaste ValideringException dersom mor oppgir barn med fødselsnummer men ikke er registrert med nyfødte ban uten far")
    void
        skalKasteValideringExceptionDersomMorOppgirBarnMedFoedselsnummerMenHarIngenNyfoedteBarnUtenFarKnyttetTilSeg() {

      // rydde testdata
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      // given
      var nyfoedt = mapper.toDto(henteNyligFoedtBarn());
      nyfoedt.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(new HashSet<>());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteFødeland(nyfoedt.getFoedselsnummer()))
          .thenReturn(FarskapsportalService.KODE_LAND_NORGE);
      when(personopplysningService.henteFolkeregisteridentifikator(nyfoedt.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      when(personopplysningService.henteFødselsdato(nyfoedt.getFoedselsnummer()))
          .thenReturn(nyfoedt.getFoedselsdato());

      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);

      doNothing()
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.oppretteFarskapserklaering(
                      MOR.getFoedselsnummer(),
                      OppretteFarskapserklaeringRequest.builder()
                          .barn(nyfoedt)
                          .opplysningerOmFar(opplysningerOmFar)
                          .build()));

      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.INGEN_NYFOEDTE_UTEN_FAR);
    }

    @Test
    @DisplayName("Skal kaste NyfoedtErForGammelException dersom nyfødt er for gammel")
    void skalKasteNyfoedtErForGammelExceptionDersomNyfoedtErForGammel() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var foedselsdatoNyfoedt =
          LocalDate.now()
              .minusMonths(
                  farskapsportalApiEgenskaper
                      .getFarskapsportalFellesEgenskaper()
                      .getMaksAntallMaanederEtterFoedsel());
      var nyfoedt = mapper.toDto(henteBarnMedFnr(foedselsdatoNyfoedt));
      nyfoedt.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);
      nyfoedt.setFoedested("Parken");
      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf = "Jeg erklærer med dette farskap til barnet..".getBytes();

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(nyfoedt.getFoedselsnummer()));
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());

      when(personopplysningService.henteFødselsdato(nyfoedt.getFoedselsnummer()))
          .thenReturn(foedselsdatoNyfoedt);
      when(personopplysningService.henteFødeland(nyfoedt.getFoedselsnummer()))
          .thenReturn(FarskapsportalService.KODE_LAND_NORGE);
      when(personopplysningService.henteFolkeregisteridentifikator(nyfoedt.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());
      when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn(pdf);

      doNothing()
          .when(difiESignaturConsumer)
          .oppretteSigneringsjobb(anyInt(), any(), any(), any(), any(), any());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.oppretteFarskapserklaering(
                      MOR.getFoedselsnummer(),
                      OppretteFarskapserklaeringRequest.builder()
                          .barn(nyfoedt)
                          .opplysningerOmFar(opplysningerOmFar)
                          .build()));

      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.NYFODT_ER_FOR_GAMMEL);
    }
  }

  @Nested
  @DisplayName("Teste oppdatereStatusSigneringsjobb")
  class OppdatereStatusSigneringsjobb {

    @Test
    @Transactional
    void skalOppdatereSigneringsinformasjonForMorEtterRedirectDersomStatusQueryTokenErGyldig() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesMor = lageUri(wiremockPort, "/padesMor");

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      lagretFarskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(null);
      farskapserklaeringDao.save(lagretFarskapserklaering);

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteGjeldendeKjoenn(MOR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.KVINNE).build());

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(
              mapper.modelMapper(MOR, Forelder.class), mapper.modelMapper(FAR, Forelder.class));

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.PAAGAAR)
                  .padeslenke(padesMor)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(MOR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      // when
      farskapsportalService.oppdatereStatusSigneringsjobb(
          MOR.getFoedselsnummer(), lagretFarskapserklaering.getId(), "etGyldigStatusQueryToken");

      // then
      verify(brukernotifikasjonConsumer, times(1))
          .oppretteOppgaveTilFarOmSignering(anyInt(), any(Forelder.class));
      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertNotNull(
                  oppdatertFarskapserklaering
                      .get()
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()));
    }

    @Test
    @Transactional
    void skalOppdatereSigneringsinformasjonForFarEtterRedirectDersomStatusQueryTokenErGyldig() {

      // given
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesFar = lageUri(wiremockPort, "/padesFar");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved farskap til dette barnet".getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);

      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      farskapserklaeringDao.save(lagretFarskapserklaering);

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteGjeldendeKjoenn(FAR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.MANN).build());

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .sletteFarsSigneringsoppgave(anyString(), any(Forelder.class));
      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(MOR, FAR);

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.SUKSESS)
                  .padeslenke(padesFar)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(FAR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(farskapserklaeringDokumentinnhold);
      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);

      // when
      farskapsportalService.oppdatereStatusSigneringsjobb(
          FAR.getFoedselsnummer(), lagretFarskapserklaering.getId(), "etGyldigStatusQueryToken");

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      // then
      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertNotNull(
                  oppdatertFarskapserklaering
                      .get()
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));
    }

    @Test
    @Transactional
    void skalOppdatereSigneringsjobbDersomMorHarAktivOgDeaktivertFarskapserklaering() {

      // given
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesMor = lageUri(wiremockPort, "/padesMor");

      var lagretDeaktivFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretDeaktivFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      lagretDeaktivFarskapserklaering.setDeaktivert(LocalDateTime.now());
      persistenceService.oppdatereFarskapserklaering(lagretDeaktivFarskapserklaering);

      var lagretAktivFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(
              henteFarskapserklaering(
                  henteForelder(Forelderrolle.MOR),
                  henteForelder(Forelderrolle.FAR),
                  henteBarnUtenFnr(5)));
      lagretAktivFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      lagretAktivFarskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(null);
      persistenceService.oppdatereFarskapserklaering(lagretAktivFarskapserklaering);

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteGjeldendeKjoenn(MOR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.KVINNE).build());

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .sletteFarsSigneringsoppgave(anyString(), any(Forelder.class));
      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(
              mapper.modelMapper(MOR, Forelder.class), mapper.modelMapper(FAR, Forelder.class));

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.PAAGAAR)
                  .padeslenke(padesMor)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(MOR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      // when
      farskapsportalService.oppdatereStatusSigneringsjobb(
          MOR.getFoedselsnummer(),
          lagretAktivFarskapserklaering.getId(),
          "etGyldigStatusQueryToken");

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretAktivFarskapserklaering.getId());

      // then
      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertNotNull(
                  oppdatertFarskapserklaering
                      .get()
                      .getDokument()
                      .getSigneringsinformasjonMor()
                      .getSigneringstidspunkt()));
    }

    @Test
    @Transactional
    void skalSletteSigneringsoppgaveNaarFarSignerer() {

      // given
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesFar = lageUri(wiremockPort, "/padesFar");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));

      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      farskapserklaeringDao.save(lagretFarskapserklaering);

      var lagretOppgavebestilling =
          oppgavebestillingDao.save(
              Oppgavebestilling.builder()
                  .farskapserklaering(lagretFarskapserklaering)
                  .forelder(lagretFarskapserklaering.getFar())
                  .eventId(UUID.randomUUID().toString())
                  .opprettet(LocalDateTime.now())
                  .build());

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteGjeldendeKjoenn(FAR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.MANN).build());

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .sletteFarsSigneringsoppgave(lagretOppgavebestilling.getEventId(), FAR);
      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(
              mapper.modelMapper(MOR, Forelder.class), mapper.modelMapper(FAR, Forelder.class));

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.SUKSESS)
                  .padeslenke(padesFar)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(FAR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      // when
      farskapsportalService.oppdatereStatusSigneringsjobb(
          FAR.getFoedselsnummer(), lagretFarskapserklaering.getId(), "etGyldigStatusQueryToken");

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      // then
      verify(brukernotifikasjonConsumer, times(1))
          .sletteFarsSigneringsoppgave(lagretOppgavebestilling.getEventId(), FAR);

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertNotNull(
                  oppdatertFarskapserklaering
                      .get()
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));
    }

    @Test
    @Transactional
    void skalSletteSigneringsoppgaveDersomFarAvbryterSignering() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUrl(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesFar = lageUri(wiremockPort, "/padesFar");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved farskap til dette barnet".getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);
      farskapserklaering.getDokument().setStatusUrl(statuslenke);
      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());
      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      var lagretOppgavebestilling =
          oppgavebestillingDao.save(
              Oppgavebestilling.builder()
                  .eventId(UUID.randomUUID().toString())
                  .forelder(lagretFarskapserklaering.getFar())
                  .farskapserklaering(lagretFarskapserklaering)
                  .opprettet(LocalDateTime.now())
                  .build());

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteGjeldendeKjoenn(FAR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.MANN).build());

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(MOR, FAR);

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(tilUri(statuslenke))
                  .statusSignering(StatusSignering.FEILET)
                  .padeslenke(padesFar)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(FAR.getFoedselsnummer())
                              .harSignert(false)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(null)
                              .build()))
                  .build());

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(farskapserklaeringDokumentinnhold);
      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);

      // when
      var esigneringStatusFeiletException =
          assertThrows(
              EsigneringStatusFeiletException.class,
              () ->
                  farskapsportalService.oppdatereStatusSigneringsjobb(
                      FAR.getFoedselsnummer(),
                      lagretFarskapserklaering.getId(),
                      "etGyldigStatusQueryToken"));

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      // then
      verify(brukernotifikasjonConsumer, times(1))
          .sletteFarsSigneringsoppgave(lagretOppgavebestilling.getEventId(), FAR);

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getStatusSignering())
                  .isEqualTo("FEILET"),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getSigneringstidspunkt())
                  .isNull(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDeaktivert()).isNotNull(),
          () ->
              assertThat(esigneringStatusFeiletException.getFeilkode())
                  .isEqualTo(Feilkode.ESIGNERING_STATUS_FEILET));
    }

    @Test
    @Transactional
    void skalIkkeBestilleFerdigstillingAvOppgaveDersomFarIkkeHarAktiveOppgaver() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUrl(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesFar = lageUrl(wiremockPort, "/padesFar");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved farskap til dette barnet".getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);

      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke);
      farskapserklaeringDao.save(lagretFarskapserklaering);

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteGjeldendeKjoenn(FAR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.MANN).build());

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(
              mapper.modelMapper(MOR, Forelder.class), mapper.modelMapper(FAR, Forelder.class));

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(tilUri(statuslenke))
                  .statusSignering(StatusSignering.FEILET)
                  .padeslenke(tilUri(padesFar))
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(FAR.getFoedselsnummer())
                              .harSignert(false)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(null)
                              .build()))
                  .build());

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(farskapserklaeringDokumentinnhold);
      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);

      // when
      var esigneringStatusFeiletException =
          assertThrows(
              EsigneringStatusFeiletException.class,
              () ->
                  farskapsportalService.oppdatereStatusSigneringsjobb(
                      FAR.getFoedselsnummer(),
                      lagretFarskapserklaering.getId(),
                      "etGyldigStatusQueryToken"));

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      // then
      verify(brukernotifikasjonConsumer, times(0))
          .sletteFarsSigneringsoppgave(anyString(), any(Forelder.class));

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getStatusSignering())
                  .isEqualTo("FEILET"),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getSigneringstidspunkt())
                  .isNull(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDeaktivert()).isNotNull(),
          () ->
              assertThat(esigneringStatusFeiletException.getFeilkode())
                  .isEqualTo(Feilkode.ESIGNERING_STATUS_FEILET));
    }

    @Test
    @Transactional
    void skalLagreStatusQueryTokenDersomStatusOppdateres() {

      // given
      farskapserklaeringDao.deleteAll();
      var statusQueryToken = "etGyldigStatusQueryToken";

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesFar = lageUri(wiremockPort, "/padesFar");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved farskap til dette barnet".getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);

      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      farskapserklaeringDao.save(lagretFarskapserklaering);

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteGjeldendeKjoenn(FAR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.MANN).build());

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .sletteFarsSigneringsoppgave(anyString(), any(Forelder.class));
      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(MOR, FAR);

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.SUKSESS)
                  .padeslenke(padesFar)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(FAR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      // when
      farskapsportalService.oppdatereStatusSigneringsjobb(
          FAR.getFoedselsnummer(), lagretFarskapserklaering.getId(), statusQueryToken);

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      // then
      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertNotNull(
                  oppdatertFarskapserklaering
                      .get()
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()),
          () ->
              assertThat(oppdatertFarskapserklaering.get().getDokument().getStatusQueryToken())
                  .isEqualTo(statusQueryToken));
    }

    @Test
    @Transactional
    void dersomMorAvbryterSigneringSkalAktuellFarskapserklaeringDeaktiveres() {

      // given
      oppgavebestillingDao.deleteAll();
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesMor = lageUri(wiremockPort, "/padesMor");

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      lagretFarskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(null);
      farskapserklaeringDao.save(lagretFarskapserklaering);

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteGjeldendeKjoenn(MOR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.KVINNE).build());

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.FEILET)
                  .padeslenke(padesMor)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(MOR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      // when
      var esigneringStatusFeiletException =
          assertThrows(
              EsigneringStatusFeiletException.class,
              () ->
                  farskapsportalService.oppdatereStatusSigneringsjobb(
                      MOR.getFoedselsnummer(),
                      lagretFarskapserklaering.getId(),
                      "etGyldigStatusQueryToken"));

      // then
      verify(brukernotifikasjonConsumer, times(0))
          .varsleOmAvbruttSignering(any(Forelder.class), any(Forelder.class));

      assertThat(esigneringStatusFeiletException.getFarskapserklaering().isPresent());
      var farskapserklaeringReturnertFraException =
          esigneringStatusFeiletException.getFarskapserklaering().get();
      assertThat(farskapserklaeringReturnertFraException.getDeaktivert()).isNotNull();
      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDeaktivert()).isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getSigneringstidspunkt())
                  .isNull());
    }

    @Test
    @Transactional
    void dersomFarAvbryterSigneringSkalAktuellFarskapserklaeringDeaktiveres() {

      // given
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesFar = lageUri(wiremockPort, "/padesFar");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));

      assertNotNull(
          farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt());
      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      farskapserklaeringDao.save(lagretFarskapserklaering);

      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.FEILET)
                  .padeslenke(padesFar)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(FAR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      // when
      var esigneringStatusFeiletException =
          assertThrows(
              EsigneringStatusFeiletException.class,
              () ->
                  farskapsportalService.oppdatereStatusSigneringsjobb(
                      FAR.getFoedselsnummer(),
                      lagretFarskapserklaering.getId(),
                      "etGyldigStatusQueryToken"));

      // then
      verify(brukernotifikasjonConsumer, times(1))
          .varsleOmAvbruttSignering(any(Forelder.class), any(Forelder.class));

      assertAll(
          () -> assertThat(esigneringStatusFeiletException.getFarskapserklaering().isPresent()));

      var farskapserklaeringReturnertFraException =
          esigneringStatusFeiletException.getFarskapserklaering().get();

      assertThat(farskapserklaeringReturnertFraException.getDeaktivert()).isNotNull();

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDeaktivert()).isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getSigneringstidspunkt())
                  .isNotNull(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .get()
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getStatusSignering())
                  .isEqualTo(StatusSignering.FEILET.toString()));
    }
  }

  @Nested
  class SynkronisereSigneringsstatusFar {

    @Test
    @Transactional
    void skalSynkronisereStatusPaaSigneringsjobbDersomFarskapserklaeringFinnes() {

      // given
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesFar = lageUri(wiremockPort, "/padesFar");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
      farskapserklaering.getDokument().setStatusQueryToken("etGyldigQueryToken");

      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      farskapserklaeringDao.save(lagretFarskapserklaering);

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteGjeldendeKjoenn(FAR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.MANN).build());

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .sletteFarsSigneringsoppgave(anyString(), any(Forelder.class));
      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(MOR, FAR);

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.SUKSESS)
                  .padeslenke(padesFar)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(FAR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      // when
      farskapsportalService.synkronisereSigneringsstatusFar(lagretFarskapserklaering.getId());

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      // then
      assertAll(
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertNotNull(
                  oppdatertFarskapserklaering
                      .get()
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));
    }

    @Test
    void skalKasteRessursIkkeFunnetExceptionDersomFarskapserklaeringIkkeFinnes() {

      // given
      farskapserklaeringDao.deleteAll();

      var statuslenke = lageUri(wiremockPort, "/status");
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var padesFar = lageUri(wiremockPort, "/padesFar");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
      farskapserklaering.getDokument().setStatusQueryToken("etGyldigQueryToken");
      var farskapserklaeringDokumentinnhold =
          "Jeg erklærer herved farskap til dette barnet".getBytes(StandardCharsets.UTF_8);
      var xadesXml =
          "<xades><signerer>12345678912</signerer></xades>".getBytes(StandardCharsets.UTF_8);

      assertNull(
          farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt());

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);
      lagretFarskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      farskapserklaeringDao.save(lagretFarskapserklaering);

      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteGjeldendeKjoenn(FAR.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennType.MANN).build());

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      doNothing()
          .when(brukernotifikasjonConsumer)
          .sletteFarsSigneringsoppgave(anyString(), any(Forelder.class));
      doNothing()
          .when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(MOR, FAR);

      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.SUKSESS)
                  .padeslenke(padesFar)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(FAR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(farskapserklaeringDokumentinnhold);
      when(difiESignaturConsumer.henteXadesXml(any())).thenReturn(xadesXml);

      // when
      var ressursIkkeFunnetException =
          assertThrows(
              RessursIkkeFunnetException.class,
              () -> farskapsportalService.synkronisereSigneringsstatusFar(10));

      var oppdatertFarskapserklaering =
          farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      // then
      assertAll(
          () ->
              assertThat(ressursIkkeFunnetException.getFeilkode())
                  .isEqualTo(Feilkode.FANT_IKKE_FARSKAPSERKLAERING),
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () ->
              assertNull(
                  oppdatertFarskapserklaering
                      .get()
                      .getDokument()
                      .getSigneringsinformasjonFar()
                      .getSigneringstidspunkt()));
    }
  }

  @Nested
  @DisplayName("HenteNyRedirectUrl")
  class HenteNyRedirectUrl {

    @Test
    @Transactional
    void skalHenteNyRedirectUrlForMorDersomMorsUndertegnerurlErRiktig() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(null);
      var undertegnerUrlMor = lageUri(wiremockPort, "/signer-url-mor");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setUndertegnerUrl(undertegnerUrlMor.toString());

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      var fnrPaaloggetPerson = MOR.getFoedselsnummer();
      var idFarskapserklaering = lagretFarskapserklaering.getId();
      var nyRedirectUrl = lageUri(wiremockPort, "/ny-redirect");

      when(difiESignaturConsumer.henteNyRedirectUrl(undertegnerUrlMor)).thenReturn(nyRedirectUrl);

      // when
      var returnertRedirectUrl =
          farskapsportalService.henteNyRedirectUrl(fnrPaaloggetPerson, idFarskapserklaering);
      var oppdatertFarskapserklaering =
          persistenceService.henteFarskapserklaeringForId(lagretFarskapserklaering.getId());

      // then
      assertAll(
          () -> assertThat(nyRedirectUrl).isEqualTo(returnertRedirectUrl),
          () ->
              assertThat(nyRedirectUrl.toString())
                  .isEqualTo(
                      oppdatertFarskapserklaering
                          .getDokument()
                          .getSigneringsinformasjonMor()
                          .getRedirectUrl()));
    }

    @Test
    @Transactional
    void skalOppdatereLagretFarskapserklaeringMedNyRedirectUrlForFar() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var undertegnerUrlFar = lageUri(wiremockPort, "/signer-url-far");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setUndertegnerUrl(undertegnerUrlFar.toString());
      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      var fnrPaaloggetPerson = FAR.getFoedselsnummer();
      var nyRedirectUrlFar = lageUri(wiremockPort, "/ny-redirect-far");
      when(difiESignaturConsumer.henteNyRedirectUrl(undertegnerUrlFar))
          .thenReturn(nyRedirectUrlFar);

      // when
      var returnertRedirectUrl =
          farskapsportalService.henteNyRedirectUrl(
              fnrPaaloggetPerson, lagretFarskapserklaering.getId());
      var oppdatertFarskapserklaering =
          persistenceService.henteFarskapserklaeringForId(lagretFarskapserklaering.getId());

      // then
      assertAll(
          () -> assertThat(nyRedirectUrlFar).isEqualTo(returnertRedirectUrl),
          () ->
              assertThat(nyRedirectUrlFar.toString())
                  .isEqualTo(
                      oppdatertFarskapserklaering
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getRedirectUrl()));
    }

    @Test
    void
        skalKasteRessursIkkeFunnetExceptionVedHentingAvNyRedirectUrlDersomFarskapserklaeringIkkeFinnes() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var fnrPaaloggetPerson = MOR.getFoedselsnummer();
      var idFarskapserklaeringSomIkkeEksisterer = 0;

      // when, then
      assertThrows(
          RessursIkkeFunnetException.class,
          () ->
              farskapsportalService.henteNyRedirectUrl(
                  fnrPaaloggetPerson, idFarskapserklaeringSomIkkeEksisterer));
    }

    @Test
    void skalKasteValideringExceptionDersomPaaloggetPersonIkkeErPartIFarskapserklaering() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var undertegnerUrlMor = lageUrl(wiremockPort, "/signer-url-mor");
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setUndertegnerUrl(undertegnerUrlMor);

      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      var fnrPaaloggetPerson = "00000000000";

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.henteNyRedirectUrl(
                      fnrPaaloggetPerson, lagretFarskapserklaering.getId()));

      assertThat(valideringException.getFeilkode())
          .isEqualTo(Feilkode.PERSON_IKKE_PART_I_FARSKAPSERKLAERING);
    }
  }

  @Nested
  @DisplayName("Teste kontrollereFar")
  class KontrollereFar {

    @Test
    @DisplayName("Skal ikke kaste exception dersom fars navn er oppgitt riktig")
    void skalIkkeKasteExceptionDersomFarsNavnErOppgittRiktig() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.erDoed(FAR.getFoedselsnummer())).thenReturn(false);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .identifikasjonsnummer(FAR.getFoedselsnummer())
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      doNothing()
          .when(personopplysningService)
          .navnekontroll(NAVN_FAR.sammensattNavn(), registrertNavnFar.sammensattNavn());

      // when, then
      assertDoesNotThrow(
          () -> farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    void skalIkkeKasteExceptionDersomFarHarForelderrolleMorEllerFar() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR_ELLER_FAR);
      when(personopplysningService.erDoed(FAR.getFoedselsnummer())).thenReturn(false);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .identifikasjonsnummer(FAR.getFoedselsnummer())
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());
      doNothing()
          .when(personopplysningService)
          .navnekontroll(NAVN_FAR.sammensattNavn(), registrertNavnFar.sammensattNavn());

      // when, then
      assertDoesNotThrow(
          () -> farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    void skalKasteValideringExceptionDersomFarHarForelderrolleMor() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      doNothing()
          .when(personopplysningService)
          .navnekontroll(NAVN_FAR.sammensattNavn(), registrertNavnFar.sammensattNavn());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.UGYLDIG_FAR);

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    void skalKasteValideringExceptionDersomFarErUnder18Aar() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(LocalDate.now().minusYears(17));
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(false);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      doNothing()
          .when(personopplysningService)
          .navnekontroll(NAVN_FAR.sammensattNavn(), registrertNavnFar.sammensattNavn());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.UGYLDIG_FAR);

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    void skalKasteValideringExceptionDersomFarHarVerge() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(NAVN_FAR.getFornavn() + " " + NAVN_FAR.getEtternavn())
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(LocalDate.now().minusYears(19));
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harVerge(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      doNothing()
          .when(personopplysningService)
          .navnekontroll(NAVN_FAR.sammensattNavn(), NAVN_FAR.sammensattNavn());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.UGYLDIG_FAR);

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    void skalKasteValideringExceptionDersomFarHarDnummer() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(LocalDate.now().minusYears(17));
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      doNothing()
          .when(personopplysningService)
          .navnekontroll(NAVN_FAR.sammensattNavn(), registrertNavnFar.sammensattNavn());
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .identifikasjonsnummer(FAR.getFoedselsnummer())
                  .type("DNR")
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.UGYLDIG_FAR);

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    @DisplayName("Skal kaste FeilNavnOppgittException dersom fars navn er oppgitt feil")
    void skalKasteFeilNavnOppgittExceptionDersomFarsNavnErOppgittFeil() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnFar = NAVN_FAR;
      var oppgittNavnPaaFar = "Borat Sagidyev";
      var registrertNavnMor = NAVN_MOR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(oppgittNavnPaaFar)
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(FolkeregisteridentifikatorDto.builder().status("I_BRUK").type("FNR").build());
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      Mockito.doThrow(FeilNavnOppgittException.class)
          .when(personopplysningService)
          .navnekontroll(opplysningerOmFar.getNavn(), registrertNavnFar.sammensattNavn());

      // when
      var tidspunktTestStart = LocalDateTime.now().minusSeconds(1);
      var feilNavnOppgittException =
          assertThrows(
              FeilNavnOppgittException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      var tidspunktTestSlutt = LocalDateTime.now().plusSeconds(1);

      // then
      assertAll(
          () ->
              assertThat(
                      feilNavnOppgittException
                          .getStatusKontrollereFarDto()
                          .get()
                          .getAntallFeiledeForsoek())
                  .isEqualTo(1),
          () ->
              assertThat(
                      feilNavnOppgittException
                          .getStatusKontrollereFarDto()
                          .get()
                          .getAntallResterendeForsoek())
                  .isEqualTo(
                      farskapsportalApiEgenskaper
                              .getFarskapsportalFellesEgenskaper()
                              .getKontrollFarMaksAntallForsoek()
                          - 1),
          () ->
              assertThat(
                      feilNavnOppgittException
                          .getStatusKontrollereFarDto()
                          .get()
                          .getTidspunktForNullstilling())
                  .isAfter(
                      tidspunktTestStart.plusDays(
                          farskapsportalApiEgenskaper
                              .getKontrollFarForsoekFornyesEtterAntallDager())),
          () ->
              assertThat(
                      feilNavnOppgittException
                          .getStatusKontrollereFarDto()
                          .get()
                          .getTidspunktForNullstilling())
                  .isBefore(
                      tidspunktTestSlutt.plusDays(
                          farskapsportalApiEgenskaper
                              .getKontrollFarForsoekFornyesEtterAntallDager())));

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    @DisplayName("Skal kaste exception dersom antall forsøk er brukt opp")
    void skalKasteExceptionDersomAntallForsoekErBruktOpp() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnFar = NAVN_FAR;
      var registrertNavnMor = NAVN_MOR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn("Borat Sagidyev")
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(FolkeregisteridentifikatorDto.builder().type("FNR").status("I_BRUK").build());
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      doThrow(FeilNavnOppgittException.class)
          .when(personopplysningService)
          .navnekontroll(opplysningerOmFar.getNavn(), registrertNavnFar.sammensattNavn());

      // when
      // Bruker opp antall mulige forsøk på å finne frem til riktig kombinasjon av fnr og navn
      for (int i = 0; i < 5; i++) {
        assertThrows(
            FeilNavnOppgittException.class,
            () -> farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));
      }

      // Sjette forsøk gir ValideringsException ettersom antall mulige forsøk er brukt opp
      var feilNavnOppgittException =
          assertThrows(
              FeilNavnOppgittException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // then
      assertThat(feilNavnOppgittException.getFeilkode()).isEqualTo(Feilkode.MAKS_ANTALL_FORSOEK);

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    void skalKasteValideringExceptionDersomMorOppgirSegSelvSomFar() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnMor = NAVN_MOR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(MOR.getFoedselsnummer())
              .navn(NAVN_MOR.sammensattNavn())
              .build();
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR_ELLER_FAR);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      doNothing()
          .when(personopplysningService)
          .navnekontroll(NAVN_FAR.sammensattNavn(), registrertNavnMor.sammensattNavn());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.MOR_OG_FAR_SAMME_PERSON);

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    void skalTelleSomForsoekDersomOppgittFnrTilFarIkkeEksisterer() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var oppgittNavnPaaFar = "Borat Sagidyev";
      var fnrIkkeEksisterende = "55555111111";
      var registrertNavnMor = NAVN_MOR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(fnrIkkeEksisterende)
              .navn(oppgittNavnPaaFar)
              .build();

      doThrow(new RessursIkkeFunnetException(Feilkode.PDL_PERSON_IKKE_FUNNET))
          .when(personopplysningService)
          .henteNavn(fnrIkkeEksisterende);
      when(personopplysningService.henteNavn(MOR.getFoedselsnummer()))
          .thenReturn(registrertNavnMor);

      // when
      var tidspunktTestStart = LocalDateTime.now().minusSeconds(1);
      var personIkkeFunnetException =
          assertThrows(
              PersonIkkeFunnetException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      var tidspunktTestSlutt = LocalDateTime.now().plusSeconds(1);

      // then
      assertAll(
          () ->
              assertThat(personIkkeFunnetException.getFeilkode())
                  .isEqualTo(Feilkode.PDL_PERSON_IKKE_FUNNET),
          () ->
              assertThat(
                      personIkkeFunnetException
                          .getStatusKontrollereFarDto()
                          .get()
                          .getAntallFeiledeForsoek())
                  .isEqualTo(1),
          () ->
              assertThat(
                      personIkkeFunnetException
                          .getStatusKontrollereFarDto()
                          .get()
                          .getAntallResterendeForsoek())
                  .isEqualTo(
                      farskapsportalApiEgenskaper
                              .getFarskapsportalFellesEgenskaper()
                              .getKontrollFarMaksAntallForsoek()
                          - 1),
          () ->
              assertThat(
                      personIkkeFunnetException
                          .getStatusKontrollereFarDto()
                          .get()
                          .getTidspunktForNullstilling())
                  .isAfter(
                      tidspunktTestStart.plusDays(
                          farskapsportalApiEgenskaper
                              .getKontrollFarForsoekFornyesEtterAntallDager())),
          () ->
              assertThat(
                      personIkkeFunnetException
                          .getStatusKontrollereFarDto()
                          .get()
                          .getTidspunktForNullstilling())
                  .isBefore(
                      tidspunktTestSlutt.plusDays(
                          farskapsportalApiEgenskaper
                              .getKontrollFarForsoekFornyesEtterAntallDager())));

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }

    @Test
    void skalKasteValideringExceptionDersomFarErDoed() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var registrertNavnFar = NAVN_FAR;
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(FAR.getFoedselsnummer())
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();
      when(personopplysningService.henteNavn(FAR.getFoedselsnummer()))
          .thenReturn(registrertNavnFar);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(LocalDate.now().minusYears(17));
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.erDoed(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      doNothing()
          .when(personopplysningService)
          .navnekontroll(NAVN_FAR.sammensattNavn(), registrertNavnFar.sammensattNavn());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.kontrollereFar(MOR.getFoedselsnummer(), opplysningerOmFar));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.UGYLDIG_FAR);

      // rydde testdata
      statusKontrollereFarDao.deleteAll();
    }
  }

  @Nested
  @DisplayName("Teste validereMor")
  class ValidereMor {

    @Test
    void skalKasteValideringExceptionDersomMorIkkeErRegistrertMedNorskBostedsadresse() {

      // given
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(false);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(FolkeregisteridentifikatorDto.builder().type("FNR").status("I_BRUK").build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode())
          .isEqualTo(Feilkode.MOR_IKKE_NORSK_BOSTEDSADRESSE);
    }

    @Test
    void personOver18AarMedFoedekjoennKvinneKanOpptreSomMor() {

      // given
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      // when, then
      assertDoesNotThrow(() -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));
    }

    @Test
    void personOver18AarMedVergeOgFoedekjoennKvinneKanIkkeOpptreSomMor() {

      // given
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.harVerge(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.FORELDER_HAR_VERGE);
    }

    @Test
    void giftPersonOver18AarMedFoedekjoennKvinneKanIkkeOpptreSomMor() {

      // given
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.GIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.MOR_SIVILSTAND_GIFT);
    }

    @Test
    void personUnder18AarMedFoedekjoennKvinneKanIkkeOpptreSomMor() {

      // given
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(false);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.GIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.IKKE_MYNDIG);
    }

    @Test
    void mannOver18AarMedFoedekjoennKvinneKanOpptreSomMor() {

      // given
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR_ELLER_FAR);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      // when, then
      assertDoesNotThrow(() -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));
    }

    @Test
    void mannOver18AarMedFoedekjoennMannKanIkkeOpptreSomMor() {

      // given
      when(personopplysningService.erOver18Aar(FAR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(FAR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.harNorskBostedsadresse(FAR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(FAR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.FAR);
      when(personopplysningService.henteFolkeregisteridentifikator(FAR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.validereMor(FAR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.FEIL_ROLLE_OPPRETTE);
    }

    @Test
    void skalKasteValideringExceptionDersomMorHarDnummer() {

      // given
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .identifikasjonsnummer(MOR.getFoedselsnummer())
                  .type("DNR")
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.MOR_HAR_IKKE_FNUMMER);
    }

    @Test
    void
        skalIkkeKasteValideringExceptionForPersonMedNyfoedtUtenRegistrertFarOgForelderrolleUkjent() {

      // given
      var nyligFoedtBarn = henteNyligFoedtBarn();

      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.UKJENT);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());
      when(personopplysningService.henteNyligFoedteBarnUtenRegistrertFar(MOR.getFoedselsnummer()))
          .thenReturn(Set.of(nyligFoedtBarn.getFoedselsnummer()));

      // when, then
      assertDoesNotThrow(() -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));
    }

    @Test
    void skalKasteValideringExceptionForPersonUtenNyfoedtUtenRegistrertFarOgForelderrolleUkjent() {

      // given
      when(personopplysningService.erOver18Aar(MOR.getFoedselsnummer())).thenReturn(true);
      when(personopplysningService.henteSivilstand(MOR.getFoedselsnummer()))
          .thenReturn(SivilstandDto.builder().type(Sivilstandtype.UGIFT).build());
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);
      when(personopplysningService.bestemmeForelderrolle(MOR.getFoedselsnummer()))
          .thenReturn(Forelderrolle.UKJENT);
      when(personopplysningService.henteFolkeregisteridentifikator(MOR.getFoedselsnummer()))
          .thenReturn(
              FolkeregisteridentifikatorDto.builder()
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .build());

      // when
      var valideringException =
          assertThrows(
              ValideringException.class,
              () -> farskapsportalService.validereMor(MOR.getFoedselsnummer()));

      // then
      assertThat(valideringException.getFeilkode()).isEqualTo(Feilkode.FEIL_ROLLE_OPPRETTE);
    }
  }

  @Nested
  class OppdaterFarskapserklaering {

    @Test
    void skalKasteValideringExceptionDersomMorForsoekerAaOppdatereFarskapserklaering() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      var fnrPaaloggetPerson = MOR.getFoedselsnummer();
      var request =
          OppdatereFarskapserklaeringRequest.builder()
              .idFarskapserklaering(lagretFarskapserklaering.getId())
              .farBorSammenMedMor(true)
              .build();

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      // when
      var valideringsException =
          assertThrows(
              ValideringException.class,
              () ->
                  farskapsportalService.oppdatereFarskapserklaeringMedFarBorSammenInfo(
                      fnrPaaloggetPerson, request));

      // then
      assertThat(valideringsException.getFeilkode())
          .isEqualTo(Feilkode.BOR_SAMMEN_INFO_KAN_BARE_OPPDATERES_AV_FAR);
    }

    @Test
    @Transactional
    void skalOppdatereBorSammeninformasjonDersomPersonErFarIFarskapserklaeringen() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      var fnrPaaloggetPerson = FAR.getFoedselsnummer();
      var request =
          OppdatereFarskapserklaeringRequest.builder()
              .idFarskapserklaering(lagretFarskapserklaering.getId())
              .farBorSammenMedMor(true)
              .build();

      when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
      when(personopplysningService.henteFødselsdato(FAR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_FAR);

      when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFødselsdato(MOR.getFoedselsnummer()))
          .thenReturn(FOEDSELSDATO_MOR);
      when(personopplysningService.harNorskBostedsadresse(MOR.getFoedselsnummer()))
          .thenReturn(true);

      // when
      var respons =
          farskapsportalService.oppdatereFarskapserklaeringMedFarBorSammenInfo(
              fnrPaaloggetPerson, request);

      // then
      var oppdatertFarskapserklaering =
          persistenceService.henteFarskapserklaeringForId(lagretFarskapserklaering.getId());

      assertAll(
          () ->
              assertThat(respons.getOppdatertFarskapserklaeringDto().getFarBorSammenMedMor())
                  .isTrue(),
          () ->
              assertThat(
                      oppdatertFarskapserklaering
                          .getDokument()
                          .getSigneringsinformasjonFar()
                          .getSendtTilSignering())
                  .isNotNull());
    }

    @Test
    void skalKasteValideringExceptionDersomPersonIkkeErPartIFarskapserklaeringeg() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering =
          henteFarskapserklaering(
              henteForelder(Forelderrolle.MOR),
              henteForelder(Forelderrolle.FAR),
              henteBarnUtenFnr(5));
      var lagretFarskapserklaering =
          persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      var fnrPaaloggetPerson = "12345678910";
      var request =
          OppdatereFarskapserklaeringRequest.builder()
              .idFarskapserklaering(lagretFarskapserklaering.getId())
              .farBorSammenMedMor(true)
              .build();

      // when, then
      assertThrows(
          ValideringException.class,
          () ->
              farskapsportalService.oppdatereFarskapserklaeringMedFarBorSammenInfo(
                  fnrPaaloggetPerson, request));
    }
  }

  @Nested
  class HenteDokumentinnhold {

    @Test
    @Transactional
    void skalIkkeHenteDokumentinnholdForErklaeringSomManglerSignaturFraBeggeForeldrene() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // given
      var statuslenke = lageUri(wiremockPort, "/status");
      var dokumenttekst =
          "Jeg erklærer herved farskap til dette barnet".getBytes(StandardCharsets.UTF_8);
      var farskapserklaering =
          persistenceService.lagreNyFarskapserklaering(
              henteFarskapserklaering(
                  henteForelder(Forelderrolle.MOR),
                  henteForelder(Forelderrolle.FAR),
                  henteBarnUtenFnr(5)));
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      persistenceService.oppdatereFarskapserklaering(farskapserklaering);

      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES))
              .name("fp-1.pdf")
              .build();
      when(bucketConsumer.lagrePades(1, dokumenttekst)).thenReturn(blobIdGcp);
      when(bucketConsumer.getContentFromBucket(any())).thenReturn(dokumenttekst);
      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.PAAGAAR)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(MOR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());
      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn("Jeg erklærer med dette farskap til barnet..".getBytes());

      // when
      var dokumentinnhold =
          farskapsportalService.henteDokumentinnhold(
              FAR.getFoedselsnummer(), farskapserklaering.getId());

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(farskapserklaering.getId());

      assertAll(
          () -> assertThat(dokumentinnhold).isNull(),
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDokument().getBlobIdGcp() != null));
    }

    @Test
    void skalKasteExceptionDersomPersonIkkeErPartIErklaeringen() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // given
      var farskapserklaering =
          farskapserklaeringDao.save(henteFarskapserklaering(MOR, FAR, henteBarnUtenFnr(5)));

      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now());
      farskapserklaeringDao.save(farskapserklaering);

      // when, then
      Farskapserklaering finalFarskapserklaering = farskapserklaering;
      assertThrows(
          ValideringException.class,
          () ->
              farskapsportalService.henteDokumentinnhold(
                  FOEDSELSDATO_FAR.format(DateTimeFormatter.ofPattern("ddMMyy")) + "35351",
                  finalFarskapserklaering.getId()));
    }

    @Test
    void skalKasteRessursIkkeFunnetExceptionForFarDersomErklaeringIkkeFinnes() {

      // given
      var idFarskapserklaeringSomIkkeFinnes = 123;

      // when, then
      assertThrows(
          RessursIkkeFunnetException.class,
          () ->
              farskapsportalService.henteDokumentinnhold(
                  FAR.getFoedselsnummer(), idFarskapserklaeringSomIkkeFinnes));
    }

    @Test
    void skalHenteDokumentForErklaeringSinertAvBeggeForeldre() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // given
      var statuslenke = lageUri(wiremockPort, "/status");
      var dokumenttekst =
          "Jeg erklærer herved farskap til dette barnet".getBytes(StandardCharsets.UTF_8);
      var farskapserklaering =
          persistenceService.lagreNyFarskapserklaering(
              henteFarskapserklaering(
                  henteForelder(Forelderrolle.MOR),
                  henteForelder(Forelderrolle.FAR),
                  henteBarnUtenFnr(5)));
      farskapserklaering.getDokument().setStatusUrl(statuslenke.toString());
      farskapserklaering
          .getDokument()
          .getSigneringsinformasjonFar()
          .setSigneringstidspunkt(LocalDateTime.now());
      persistenceService.oppdatereFarskapserklaering(farskapserklaering);

      var blobIdGcp =
          BlobIdGcp.builder()
              .bucket(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES))
              .name("fp-1.pdf")
              .build();
      when(bucketConsumer.lagrePades(1, dokumenttekst)).thenReturn(blobIdGcp);
      when(bucketConsumer.getContentFromBucket(any())).thenReturn(dokumenttekst);
      when(difiESignaturConsumer.henteStatus(any(), any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .bekreftelseslenke(lageUri(wiremockPort, "/confirmation"))
                  .statuslenke(statuslenke)
                  .statusSignering(StatusSignering.PAAGAAR)
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(MOR.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                              .xadeslenke(lageUri(wiremockPort, "/xades"))
                              .build()))
                  .build());
      when(difiESignaturConsumer.henteSignertDokument(any())).thenReturn(dokumenttekst);

      // when
      var dokumentinnhold =
          farskapsportalService.henteDokumentinnhold(
              MOR.getFoedselsnummer(), farskapserklaering.getId());

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(farskapserklaering.getId());
      assertAll(
          () -> assertThat(dokumentinnhold).isNotNull(),
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDokument().getBlobIdGcp() != null));
    }

    @Test
    void skalKasteRessursIkkeFunnetExceptionForMorUtenAktiveErklaeringer() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var idFarskapserklaeringSomIkkeFinnes = 1525;

      // when, then
      assertThrows(
          RessursIkkeFunnetException.class,
          () ->
              farskapsportalService.henteDokumentinnhold(
                  FAR.getFoedselsnummer(), idFarskapserklaeringSomIkkeFinnes));
    }
  }
}
