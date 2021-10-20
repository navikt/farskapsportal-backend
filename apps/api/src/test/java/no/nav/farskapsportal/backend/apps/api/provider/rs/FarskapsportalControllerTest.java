package no.nav.farskapsportal.backend.apps.api.provider.rs;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK;
import static no.nav.farskapsportal.backend.libs.felles.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteNyligFoedtBarn;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUri;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.tilUri;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
<<<<<<< HEAD:apps/api/src/test/java/no/nav/farskapsportal/backend/apps/api/provider/rs/FarskapsportalControllerTest.java
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.api.BrukerinformasjonResponse;
import no.nav.farskapsportal.backend.apps.api.api.FarskapserklaeringFeilResponse;
import no.nav.farskapsportal.backend.apps.api.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.backend.apps.api.api.OppdatereFarskapserklaeringRequest;
import no.nav.farskapsportal.backend.apps.api.api.OppdatereFarskapserklaeringResponse;
import no.nav.farskapsportal.backend.apps.api.api.OppretteFarskapserklaeringRequest;
import no.nav.farskapsportal.backend.apps.api.api.OppretteFarskapserklaeringResponse;
import no.nav.farskapsportal.backend.apps.api.api.Skriftspraak;
import no.nav.farskapsportal.backend.apps.api.api.StatusSignering;
import no.nav.farskapsportal.backend.apps.api.config.FarskapsportalApiConfig.OidcTokenSubjectExtractor;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalApiEgenskaper;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.backend.apps.api.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.backend.apps.api.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.backend.libs.dto.BarnDto;
import no.nav.farskapsportal.backend.libs.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.FolkeregisteridentifikatorDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonRolle;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonRolle.Sivilstandtype;
import no.nav.farskapsportal.backend.libs.dto.pdl.KjoennType;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.BostedsadresseDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.UtenlandskAdresseDto;
import no.nav.farskapsportal.backend.libs.dto.pdl.bostedsadresse.VegadresseDto;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.exception.EsigneringConsumerException;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.ForelderDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonBostedsadresse;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonDoedsfall;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonFoedsel;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonFolkeregisteridentifikator;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonForelderBarnRelasjon;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonSivilstand;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.sts.stub.StsStub;
import no.nav.farskapsportal.backend.libs.felles.util.Mapper;
import org.assertj.core.api.AssertionsForClassTypes;
=======
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.BrukerinformasjonResponse;
import no.nav.farskapsportal.api.FarskapserklaeringFeilResponse;
import no.nav.farskapsportal.api.Feilkode;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppdatereFarskapserklaeringRequest;
import no.nav.farskapsportal.api.OppdatereFarskapserklaeringResponse;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringRequest;
import no.nav.farskapsportal.api.OppretteFarskapserklaeringResponse;
import no.nav.farskapsportal.api.Sivilstandtype;
import no.nav.farskapsportal.api.Skriftspraak;
import no.nav.farskapsportal.api.StatusSignering;
import no.nav.farskapsportal.config.FarskapsportalConfig.OidcTokenSubjectExtractor;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.FolkeregisteridentifikatorDto;
import no.nav.farskapsportal.consumer.pdl.api.ForelderBarnRelasjonDto;
import no.nav.farskapsportal.consumer.pdl.api.ForelderBarnRelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.BostedsadresseDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.UtenlandskAdresseDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.VegadresseDto;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonBostedsadresse;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonDoedsfall;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFoedsel;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFolkeregisteridentifikator;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonForelderBarnRelasjon;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSivilstand;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.NavnDto;
import no.nav.farskapsportal.exception.EsigneringConsumerException;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.persistence.dao.StatusKontrollereFarDao;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.persistence.entity.Oppgavebestilling;
import no.nav.farskapsportal.persistence.entity.Signeringsinformasjon;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.util.Mapper;
>>>>>>> main:src/test/java/no/nav/farskapsportal/provider/rs/FarskapsportalControllerTest.java
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

@DisplayName("FarskapsportalController")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalApiApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 8096)
public class FarskapsportalControllerTest {

  private static final Forelder MOR = henteForelder(Forelderrolle.MOR);
  private static final Forelder FAR = henteForelder(Forelderrolle.FAR);
  private static final NavnDto NAVN_MOR = NavnDto.builder().fornavn("Dolly").etternavn("Duck").build();
  private static final NavnDto NAVN_FAR = NavnDto.builder().fornavn("Fetter").etternavn("Anton").build();

  private static final Barn BARN_UTEN_FNR = henteBarnUtenFnr(5);
  private static final Barn BARN_MED_FNR = henteNyligFoedtBarn();

  private static final KontrollerePersonopplysningerRequest KONTROLLEREOPPLYSNINGER_OM_FAR = KontrollerePersonopplysningerRequest.builder()
      .foedselsnummer(FAR.getFoedselsnummer()).navn(NAVN_FAR.sammensattNavn()).build();
  private static final LinkedHashMap<KjoennType, LocalDateTime> KJOENNSHISTORIKK_MOR = getKjoennshistorikk(KjoennType.KVINNE);
  private static final LinkedHashMap<KjoennType, LocalDateTime> KJOENNSHISTORIKK_FAR = getKjoennshistorikk(KjoennType.MANN);
  private static final BostedsadresseDto BOSTEDSADRESSE = getBostedsadresse(true);
  private static final String REDIRECT_URL = "https://redirect.mot.signeringstjensesten.settes.under.normal.kjoering.etter.opprettelse.av.signeringsjobb.no";

  @LocalServerPort
  private int localServerPort;
  @Autowired
  private HttpHeaderTestRestTemplate httpHeaderTestRestTemplate;
  @Autowired
  private StsStub stsStub;
  @Autowired
  private PdlApiStub pdlApiStub;
  @MockBean
  private OidcTokenSubjectExtractor oidcTokenSubjectExtractor;
  @MockBean
  private PdfGeneratorConsumer pdfGeneratorConsumer;
  @MockBean
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  @MockBean
  private DifiESignaturConsumer difiESignaturConsumer;
  @Autowired
  private PersistenceService persistenceService;
  @Autowired
  private OppgavebestillingDao oppgavebestillingDao;
  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;
  @Autowired
  private ForelderDao forelderDao;
  @Autowired
  private StatusKontrollereFarDao statusKontrollereFarDao;
  @Autowired
  private FarskapsportalApiEgenskaper farskapsportalApiEgenskaper;
  @Autowired
  private Mapper mapper;

  static <T> HttpEntity<T> initHttpEntity(T body, CustomHeader... customHeaders) {

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    if (customHeaders != null) {
      for (var header : customHeaders) {
        headers.add(header.headerName, header.headerValue);
      }
    }

    return new HttpEntity<>(body, headers);
  }

  private static LinkedHashMap<KjoennType, LocalDateTime> getKjoennshistorikk(KjoennType kjoennType) {
    var kjoennshistorikk = new LinkedHashMap<KjoennType, LocalDateTime>();
    kjoennshistorikk.put(kjoennType, LocalDateTime.now());

    return kjoennshistorikk;
  }

  private static BostedsadresseDto getBostedsadresse(boolean erNorsk) {
    if (erNorsk) {
      return BostedsadresseDto.builder().vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").build()).build();
    } else {
      return BostedsadresseDto.builder().utenlandskAdresse(UtenlandskAdresseDto.builder().adressenavnNummer("Stortingsgatan 14").build()).build();
    }
  }

  private String initHenteBrukerinformasjon() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/brukerinformasjon";
  }

  private String initKontrollereOpplysningerFar() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/personopplysninger/far";
  }

  private String initNyFarskapserklaering() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/farskapserklaering/ny";
  }

  private String initHenteDokumentEtterRedirect() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/farskapserklaering/redirect";
  }

  private String initHenteNyRedirectUrl() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/redirect-url/ny";
  }

  private String initOppdatereFarskapserklaering() {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/farskapserklaering/oppdatere";
  }

  private String initHenteDokumentinnhold(int idFarskapserklaering) {
    return getBaseUrlForStubs() + "/api/v1/farskapsportal/farskapserklaering/" + idFarskapserklaering + "/dokument";
  }

  private String getBaseUrlForStubs() {
    return "http://localhost:" + localServerPort;
  }

  private void brukeStandardMocks(String fnrPaaloggetBruker) {
    var sivilstandMor = Sivilstandtype.UGIFT;

    brukeStandardMocksUtenPdlApi(fnrPaaloggetBruker);
    brukeStandardMocksPdlApiAngiSivilstandMor(sivilstandMor);
  }

  private void brukeStandardMocksPdlApiAngiSivilstandMor(Sivilstandtype sivilstandMor) {
    pdlApiStub.runPdlApiHentPersonStub(
        List.of(
            new HentPersonKjoenn(KJOENNSHISTORIKK_MOR),
            new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
            new HentPersonSivilstand(sivilstandMor),
            new HentPersonBostedsadresse(BOSTEDSADRESSE),
            new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
            new HentPersonDoedsfall(null),
            new HentPersonBostedsadresse(BostedsadresseDto.builder()
                .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
                .build()),
            new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR).build())),
        MOR.getFoedselsnummer());

    pdlApiStub.runPdlApiHentPersonStub(
        List.of(new HentPersonKjoenn(KJOENNSHISTORIKK_FAR),
            new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
            new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
            new HentPersonDoedsfall(null),
            new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR).build())),
        KONTROLLEREOPPLYSNINGER_OM_FAR.getFoedselsnummer());
  }

  private void brukeStandardMocksUtenPdlApi(String fnrPaaloggetBruker) {

    stsStub.runSecurityTokenServiceStub("jalla");

    when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(fnrPaaloggetBruker);

    when(pdfGeneratorConsumer.genererePdf(any(), any(), any(), any())).thenReturn("Jeg erklærer med dette farskap til barnet..".getBytes());
    doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any(), any());
  }

  @AfterEach
  void ryddeTestdata() {
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();
    statusKontrollereFarDao.deleteAll();
    forelderDao.deleteAll();
  }

  private static class CustomHeader {

    String headerName;
    String headerValue;

    CustomHeader(String headerName, String headerValue) {
      this.headerName = headerName;
      this.headerValue = headerValue;
    }
  }

  @Nested
  @DisplayName("Teste henteBrukerinformasjon")
  class HenteBrukerinformasjon {

    @BeforeEach
    void ryddeTestdata() {
      farskapserklaeringDao.deleteAll();
      statusKontrollereFarDao.deleteAll();
      forelderDao.deleteAll();
    }

    @Test
    @DisplayName("Skal liste nylig fødte barn uten registrert far ved henting av brukerinformasjon for mor")
    void skalListeNyligFoedteBarnUtenRegistrertFarVedHentingAvBrukerinformasjonForMor() {

      // given
      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrSpedbarn = foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "00000";
      var foedselsdatoMor = foedselsdatoSpedbarn.minusYears(28).minusMonths(2).minusDays(13);
      var fnrMor = foedselsdatoMor.format(DateTimeFormatter.ofPattern("ddMMyy")) + "12340";

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);
      stsStub.runSecurityTokenServiceStub("jalla");
      var morsRelasjonTilBarn = ForelderBarnRelasjonDto.builder().minRolleForPerson(ForelderBarnRelasjonRolle.MOR)
          .relatertPersonsRolle(ForelderBarnRelasjonRolle.BARN).relatertPersonsIdent(fnrSpedbarn).build();
      var spedbarnetsRelasjonTilMor = ForelderBarnRelasjonDto.builder().relatertPersonsRolle(ForelderBarnRelasjonRolle.MOR)
          .relatertPersonsIdent(fnrMor)
          .minRolleForPerson(ForelderBarnRelasjonRolle.BARN).build();

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonForelderBarnRelasjon(morsRelasjonTilBarn, "123"),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
                  .build()),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          fnrMor);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonForelderBarnRelasjon(spedbarnetsRelasjonTilMor, "000"),
              new HentPersonFoedsel(foedselsdatoSpedbarn, false)),
          fnrSpedbarn);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(fnrMor);

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      var brukerinformasjonResponse = respons.getBody();

      // then
      assertAll(
          () -> assertEquals(HttpStatus.OK.value(), respons.getStatusCode().value()),
          () -> assertEquals(Forelderrolle.MOR, brukerinformasjonResponse.getForelderrolle(), "Mor skal ha forelderrolle MOR"),
          () -> assertEquals(1, brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar().size(),
              "Lista over nyfødte barn uten registrert far skal inneholde ett element"),
          () -> assertEquals(fnrSpedbarn, brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar().iterator().next(),
              "Spedbarnet i lista over nyfødte barn uten registrert far skal ha riktig fødselsnummer"));
    }

    @Test
    @DisplayName("Skal liste farskapserklæringer som venter på fars signatur ved henting av brukerinformasjon for mor")
    void skalListeFarskapserklaeringerSomVenterPaaFarVedHentingAvBrukerinformasjonForMor() {

      // given
      var farskapserklaeringSomVenterPaaFar = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));

      farskapserklaeringSomVenterPaaFar.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFar);

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonForelderBarnRelasjon(null, null),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
                  .build()),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      var brukerinformasjonResponse = respons.getBody();

      // then
      assertAll(() -> assertEquals(HttpStatus.OK.value(), respons.getStatusCode().value()),
          () -> assertEquals(Forelderrolle.MOR, brukerinformasjonResponse.getForelderrolle(), "Mor skal ha forelderrolle MOR"),
          () -> assertEquals(1, brukerinformasjonResponse.getAvventerSigneringMotpart().size(),
              "Det er en farskapserklæring som venter på fars signatur"),
          () -> Assertions.assertNull(brukerinformasjonResponse.getAvventerSigneringMotpart().iterator().next().getDokument().getSignertAvFar(),
              "Far har ikke signert farskapserklæringen"), () -> Assertions.assertEquals(FAR.getFoedselsnummer(),
              brukerinformasjonResponse.getAvventerSigneringMotpart().iterator().next().getFar().getFoedselsnummer(),
              "Farskapserklæringen gjelder riktig far"),
          () -> assertEquals(0, brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar().size()),
          () -> assertEquals(0, brukerinformasjonResponse.getAvventerSigneringBruker().size()));

      // cleanup db
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @Test
    @DisplayName("Skal liste farskapserklæringer som venter på mors signatur ved henting av brukerinformasjon for mor")
    void skalListeFarskapserklaeringerSomVenterPaaMorVedHentingAvBrukerinformasjonForMor() {

      // given
      var farskapserklaeringSomVenterPaaMor = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaMor.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(null);
      farskapserklaeringSomVenterPaaMor.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
     var lagretFarskapserklaering =  persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaMor);

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonForelderBarnRelasjon(null, null),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
                  .build()),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      var brukerinformasjonResponse = respons.getBody();

      // then
      assertAll(() -> assertEquals(HttpStatus.OK.value(), respons.getStatusCode().value()),
          () -> assertEquals(Forelderrolle.MOR, brukerinformasjonResponse.getForelderrolle(), "Mor skal ha forelderrolle MOR"),
          () -> assertEquals(1, brukerinformasjonResponse.getAvventerSigneringBruker().size()),
          () -> assertNull(brukerinformasjonResponse.getAvventerSigneringBruker().iterator().next().getDokument().getSignertAvMor()),
          () -> Assertions.assertEquals(FAR.getFoedselsnummer(),
              brukerinformasjonResponse.getAvventerSigneringBruker().iterator().next().getFar().getFoedselsnummer()),
          () -> assertEquals(0, brukerinformasjonResponse.getAvventerSigneringMotpart().size()),
          () -> assertEquals(0, brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar().size()));

      // cleanup db
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @Test
    @DisplayName("Skal liste farskapserklæringer som venter på far ved henting av brukerinformasjon for far")
    void skalListeFarskapserklaeringerSomVenterPaaFarVedHentingAvBrukerinformasjonForFar() {

      // given
      var farskapserklaeringSomVenterPaaFar = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));

      farskapserklaeringSomVenterPaaFar.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFar);

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.MANN);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonForelderBarnRelasjon(null, null),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonForelderBarnRelasjon(null, null),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          MOR.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      var brukerinformasjonResponse = respons.getBody();

      // then
      assertAll(() -> assertEquals(HttpStatus.OK.value(), respons.getStatusCodeValue()),
          () -> assertEquals(Forelderrolle.FAR, brukerinformasjonResponse.getForelderrolle(), "Far skal ha forelderrolle FAR"),
          () -> assertEquals(0, brukerinformasjonResponse.getAvventerSigneringMotpart().size(),
              "Det er en farskapserklæring som venter på fars signatur"),
          () -> assertEquals(1, brukerinformasjonResponse.getAvventerSigneringBruker().size()), () -> Assertions.assertEquals(FAR.getFoedselsnummer(),
              brukerinformasjonResponse.getAvventerSigneringBruker().iterator().next().getFar().getFoedselsnummer(),
              "Farskapserklæringen gjelder riktig far"),
          () -> assertEquals(0, brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar().size()));

    }

    @Test
    @DisplayName("Farskapserklæringer som venter på mor skal ikke dukke opp i fars liste")
    void skalIkkeListeFarskapserklaeringerSomVenterPaaMorVedHentingAvBrukerinformasjonForFar() {

      // given
      var farskapserklaeringSomVenterPaaMor = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaMor.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(null);
      farskapserklaeringSomVenterPaaMor.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(null);

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaMor);

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.MANN);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonForelderBarnRelasjon(null, null),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))), FAR.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      var brukerinformasjonResponse = respons.getBody();

      // then
      assertAll(() -> assertEquals(0, brukerinformasjonResponse.getAvventerSigneringBruker().size()),
          () -> assertEquals(0, brukerinformasjonResponse.getAvventerSigneringMotpart().size()),
          () -> assertEquals(0, brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar().size()));

      // cleanup db
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @Test
    @DisplayName("Skal gi not found dersom person ikke eksisterer")
    void skalGiNotFoundDersomPersonIkkeEksisterer() {

      // given
      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrSpedbarn = foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "00000";
      var foedselsdatoMor = foedselsdatoSpedbarn.minusYears(28).minusMonths(2).minusDays(13);
      var fnrMor = foedselsdatoMor.format(DateTimeFormatter.ofPattern("ddMMyy")) + "12340";

      var kjoennshistorikk = new LinkedHashMap<KjoennType, LocalDateTime>();
      stsStub.runSecurityTokenServiceStub("jalla");

      var spedbarnetsRelasjonTilMor = ForelderBarnRelasjonDto.builder().relatertPersonsRolle(ForelderBarnRelasjonRolle.MOR)
          .relatertPersonsIdent(fnrMor)
          .minRolleForPerson(ForelderBarnRelasjonRolle.BARN).build();

      pdlApiStub.runPdlApiHentPersonStub(List.of(new HentPersonForelderBarnRelasjon(null, ""), new HentPersonKjoenn(kjoennshistorikk)), fnrMor);
      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonForelderBarnRelasjon(spedbarnetsRelasjonTilMor, "000"), new HentPersonFoedsel(foedselsdatoSpedbarn, false)),
          fnrSpedbarn);
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(fnrMor);

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      var brukerinformasjonResponse = respons.getBody();

      // then
      assertAll(() -> assertEquals(HttpStatus.NOT_FOUND.value(), respons.getStatusCode().value()),
          () -> assertFalse(brukerinformasjonResponse.isKanOppretteFarskapserklaering()),
          () -> assertNull(brukerinformasjonResponse.getForelderrolle()), () -> assertNull(brukerinformasjonResponse.getAvventerSigneringMotpart()),
          () -> assertNull(brukerinformasjonResponse.getAvventerSigneringBruker()),
          () -> assertNull(brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar()));
    }

    @Test
    void valideringFeilerDersomMorErBosattUtenforNorge() {

      // given
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonForelderBarnRelasjon(null, null),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .utenlandskAdresse(UtenlandskAdresseDto.builder().adressenavnNummer("Parkway Avenue 123").bySted("Newcastle").landkode("US").build())
                  .build()),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      // then
      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
          () -> assertThat(respons.getBody().isKanOppretteFarskapserklaering()).isFalse()
      );
    }

    @Test
    @DisplayName("Far skal se liste over signerte farskapserklæringer som venter på registrering hos Skatt")
    void farSkalSeListeOverSignerteFarskapserklaeringerSomVenterPaaRegistrering() {

      // given
      brukeStandardMocks(FAR.getFoedselsnummer());

      var signertFarskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      signertFarskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
      signertFarskapserklaering.getDokument().setDokumentinnhold(
          Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet".getBytes(StandardCharsets.UTF_8)).build());

     var ny = persistenceService.lagreNyFarskapserklaering(signertFarskapserklaering);

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      var brukerinformasjonResponse = respons.getBody();

      // then
      assertAll(() -> assertTrue(respons.getStatusCode().is2xxSuccessful()),
          () -> assertFalse(brukerinformasjonResponse.isKanOppretteFarskapserklaering()),
          () -> assertEquals(Forelderrolle.FAR, brukerinformasjonResponse.getForelderrolle()),
          () -> assertEquals(1, brukerinformasjonResponse.getAvventerRegistrering().size()));
    }

    @Test
    @DisplayName("Bruker med forelderrolle MOR_ELLER_FAR skal se ventende farskapserklæringer for både mor og far")
    void brukerMedForelderrolleMorEllerFarSkalSeVentendeFarskapserklaeringerForBaadeMorOgFar() {

      var enAnnenMor = Forelder.builder().foedselsnummer("01010144444").build();
      var enAnnenMorsFoedselsdato = LocalDate.now().minusYears(30);
      var enAnnenMorsNavn = NavnDto.builder().fornavn("Høne").etternavn("Mor").build();
      var kvinneSomHarSkiftetKjoenn = henteForelder(Forelderrolle.MOR);
      var foedselsdatoKvinneSomHarSkiftetKjoenn = FOEDSELSDATO_MOR;

      // given
      var farskapserklaeringSomVenterPaaFarMedFoedekjoennKvinne = henteFarskapserklaering(enAnnenMor, kvinneSomHarSkiftetKjoenn,
          henteBarnUtenFnr(5));
      farskapserklaeringSomVenterPaaFarMedFoedekjoennKvinne.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusDays(3));

      farskapserklaeringSomVenterPaaFarMedFoedekjoennKvinne.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarMedFoedekjoennKvinne);

      var farskapserklaeringMedMorSomNaaErMannVenterPaaFarsSignering = henteFarskapserklaering(kvinneSomHarSkiftetKjoenn, FAR,
          BARN_UTEN_FNR);
      farskapserklaeringMedMorSomNaaErMannVenterPaaFarsSignering.getDokument().getSigneringsinformasjonMor()
          .setSigneringstidspunkt(LocalDateTime.now().minusDays(3));

      farskapserklaeringMedMorSomNaaErMannVenterPaaFarsSignering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      persistenceService.lagreNyFarskapserklaering(farskapserklaeringMedMorSomNaaErMannVenterPaaFarsSignering);

      var kjoennshistorikk = new LinkedHashMap<KjoennType, LocalDateTime>();
      kjoennshistorikk.put(KjoennType.KVINNE, LocalDateTime.now().minusYears(9));
      kjoennshistorikk.put(KjoennType.MANN, LocalDateTime.now().minusYears(2));

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonForelderBarnRelasjon(null, null),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonFoedsel(foedselsdatoKvinneSomHarSkiftetKjoenn, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
                  .build()),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          kvinneSomHarSkiftetKjoenn.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(enAnnenMorsFoedselsdato, false),
              new HentPersonNavn(mapper.modelMapper(enAnnenMorsNavn, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          enAnnenMor.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(kvinneSomHarSkiftetKjoenn.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteBrukerinformasjon(), HttpMethod.GET, null, BrukerinformasjonResponse.class);

      var brukerinformasjonResponse = respons.getBody();

      // then
      assertAll(
          () -> assertThat(respons.getStatusCode().is2xxSuccessful()).isTrue(),
          () -> assertThat(brukerinformasjonResponse.isKanOppretteFarskapserklaering()).isTrue(),
          () -> assertThat(brukerinformasjonResponse.getForelderrolle()).isEqualTo(Forelderrolle.MOR_ELLER_FAR),
          () -> assertThat(brukerinformasjonResponse.getAvventerSigneringBruker().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjonResponse.getAvventerSigneringMotpart().size()).isEqualTo(1));
    }
  }

  @Nested
  @DisplayName("Teste kontrollereOpplysningerFar")
  class KontrollereOpplysningerFar {

    @BeforeEach
    void ryddeTestdata() {
      farskapserklaeringDao.deleteAll();
      statusKontrollereFarDao.deleteAll();
      forelderDao.deleteAll();
    }

    @Test
    @DisplayName("Skal gi Ok dersom navn og kjønn er riktig")
    void skalGiOkDersomNavnOgKjoennErRiktig() {

      // given
      var fnrFar = "01057244444";
      var fornavnFar = "Borat";
      var etternavnFar = "Sagidiyev";
      var registrertNavn = no.nav.farskapsportal.backend.libs.dto.NavnDto.builder().fornavn(fornavnFar).etternavn(etternavnFar).build();
      stsStub.runSecurityTokenServiceStub("jalla");

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkMor = getKjoennshistorikk(KjoennType.KVINNE);
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkFar),
              new HentPersonNavn(mapper.modelMapper(registrertNavn, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonDoedsfall(null),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          fnrFar);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkMor),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
                  .build()),
              new HentPersonDoedsfall(null),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initKontrollereOpplysningerFar(), HttpMethod.POST,
          initHttpEntity(KontrollerePersonopplysningerRequest.builder().foedselsnummer(fnrFar).navn(fornavnFar + " " + etternavnFar).build()),
          HttpStatus.class);

      // then
      assertTrue(respons.getStatusCode().is2xxSuccessful());
    }

    @Test
    void skalGiOkDersomNavnMedDobbeltFornavnSpesialtegnOgKjoennErRiktig() {

      // given
      var fnrFar = "01057244444";
      var fornavnFar = "Borat André";
      var etternavnFar = "Sagidiyév Goliat Motreal";
      var registrertNavn = no.nav.farskapsportal.backend.libs.dto.NavnDto.builder().fornavn(fornavnFar).etternavn(etternavnFar).build();
      stsStub.runSecurityTokenServiceStub("jalla");

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkMor = getKjoennshistorikk(KjoennType.KVINNE);
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkFar),
              new HentPersonNavn(mapper.modelMapper(registrertNavn, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonDoedsfall(null),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          fnrFar);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkMor),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
                  .build()),
              new HentPersonDoedsfall(null),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initKontrollereOpplysningerFar(), HttpMethod.POST,
          initHttpEntity(KontrollerePersonopplysningerRequest.builder().foedselsnummer(fnrFar).navn(fornavnFar + " " + etternavnFar).build()),
          HttpStatus.class);

      // then
      assertTrue(respons.getStatusCode().is2xxSuccessful());
    }

    @Test
    @DisplayName("Skal gi bad request dersom oppgitt far er kvinne")
    void skalGiBadRequestDersomOppgittFarErKvinne() {

      // given
      var oppgittNavn = NAVN_MOR;

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonNavn(mapper.modelMapper(oppgittNavn, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonDoedsfall(null)));

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initKontrollereOpplysningerFar(), HttpMethod.POST,
          initHttpEntity(KontrollerePersonopplysningerRequest.builder().foedselsnummer("01058011444").navn("Natalya Sagdiyev").build()),
          String.class);

      // then
      assertTrue(respons.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Skal gi bad request dersom navn er gjengitt feil i spørring")
    void skalGiBadRequestDersomNavnErGjengittFeilISpoerring() {

      // given
      var registrertNavn = no.nav.farskapsportal.backend.libs.dto.NavnDto.builder().fornavn("Borat").etternavn("Sagdiyev").build();

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      stsStub.runSecurityTokenServiceStub("jalla");

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(registrertNavn, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonDoedsfall(null)));

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initKontrollereOpplysningerFar(), HttpMethod.POST,
          initHttpEntity(KontrollerePersonopplysningerRequest.builder().foedselsnummer("01058011444").navn("Borat Nicolai Sagdiyev").build()),
          String.class);

      // then
      assertTrue(respons.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Skal gi not found dersom oppgitt far ikke eksisterer i PDL")
    void skalGiNotFoundDersomOppgittFarIkkeEksistererIPdl() {

      // given
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");
      pdlApiStub.runPdlApiHentPersonStub(List.of(new HentPersonFoedsel(FOEDSELSDATO_MOR, false)));
      pdlApiStub.runPdlApiHentPersonFantIkkePersonenStub();

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initKontrollereOpplysningerFar(), HttpMethod.POST,
          initHttpEntity(KontrollerePersonopplysningerRequest.builder().foedselsnummer("01058011444").navn("Borat Sagdiyev").build()), String.class);

      // then
      assertSame(respons.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void skalViseAntallResterendeForsoekDersomFeilNavnOppgis() {

      // given
      var registrertNavnFar = no.nav.farskapsportal.backend.libs.dto.NavnDto.builder().fornavn("Borat").etternavn("Sagdiyev").build();
      var oppgittNavnFar = "Borat Nicolai Sagdiyev";
      var fnrFar = "01058011444";

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      stsStub.runSecurityTokenServiceStub("jalla");

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkMor = getKjoennshistorikk(KjoennType.KVINNE);
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkMor),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status("I_BRUK").type("FNR").build()),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonBostedsadresse(
                  BostedsadresseDto.builder().vegadresse(VegadresseDto.builder().adressenavn("Snarveien 15").build()).build()),
              new HentPersonDoedsfall(null)), MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkFar),
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status("I_BRUK").type("FNR").build()),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonNavn(mapper.modelMapper(registrertNavnFar, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonBostedsadresse(
                  BostedsadresseDto.builder().vegadresse(VegadresseDto.builder().adressenavn("Langata 5").build()).build()),
              new HentPersonDoedsfall(null)), fnrFar);

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initKontrollereOpplysningerFar(), HttpMethod.POST,
          initHttpEntity(KontrollerePersonopplysningerRequest.builder().foedselsnummer(fnrFar).navn(oppgittNavnFar).build()),
          String.class);

      // then
      assertTrue(respons.getStatusCode().is4xxClientError());
    }

    @Test
    void skalGiBadRequestDersomAntallForsoekErBruktOpp() {

      // given
      var registrertNavnFar = no.nav.farskapsportal.backend.libs.dto.NavnDto.builder().fornavn("Borat").etternavn("Sagdiyev").build();
      var oppgittNavnFar = "Borat Nicolai Sagdiyev";
      var fnrFar = "01058011444";

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      stsStub.runSecurityTokenServiceStub("jalla");

      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkMor = getKjoennshistorikk(KjoennType.KVINNE);
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkMor),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status("I_BRUK").type("FNR").build()),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonBostedsadresse(
                  BostedsadresseDto.builder().vegadresse(VegadresseDto.builder().adressenavn("Snarveien 15").build()).build()),
              new HentPersonDoedsfall(null)), MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkFar),
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status("I_BRUK").type("FNR").build()),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonNavn(mapper.modelMapper(registrertNavnFar, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonBostedsadresse(
                  BostedsadresseDto.builder().vegadresse(VegadresseDto.builder().adressenavn("Langata 5").build()).build()),
              new HentPersonDoedsfall(null)), fnrFar);

      // when
      for (int i = 1; i <= farskapsportalApiEgenskaper.getKontrollFarMaksAntallForsoek(); i++) {
        var respons = httpHeaderTestRestTemplate.exchange(initKontrollereOpplysningerFar(), HttpMethod.POST,
            initHttpEntity(KontrollerePersonopplysningerRequest.builder().foedselsnummer(fnrFar).navn(oppgittNavnFar).build()),
            FarskapserklaeringFeilResponse.class);

        // then
        int finalI = i;

        assertAll(
            () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
            () -> assertThat(respons.getBody().getAntallResterendeForsoek().get()).isEqualTo(
                farskapsportalApiEgenskaper.getKontrollFarMaksAntallForsoek() - finalI),
            () -> assertThat(respons.getBody().getFeilkode()).isEqualTo(Feilkode.NAVN_STEMMER_IKKE_MED_REGISTER)
        );
      }

      // when
      var tidspunktSisteForsoek = LocalDateTime.now();
      var resultatEtterOppbruktAntallForsoek = httpHeaderTestRestTemplate.exchange(initKontrollereOpplysningerFar(), HttpMethod.POST,
          initHttpEntity(KontrollerePersonopplysningerRequest.builder().foedselsnummer(fnrFar).navn(oppgittNavnFar).build()),
          FarskapserklaeringFeilResponse.class);

      // then
      assertAll(
          () -> assertThat(resultatEtterOppbruktAntallForsoek.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
          () -> assertThat(resultatEtterOppbruktAntallForsoek.getBody().getAntallResterendeForsoek().get()).isEqualTo(0),
          () -> assertThat(resultatEtterOppbruktAntallForsoek.getBody().getFeilkode()).isEqualTo(Feilkode.MAKS_ANTALL_FORSOEK),
          () -> assertThat(resultatEtterOppbruktAntallForsoek.getBody().getTidspunktForNullstillingAvForsoek()).isBefore(
              tidspunktSisteForsoek.plusDays(farskapsportalApiEgenskaper.getKontrollFarForsoekFornyesEtterAntallDager()))
      );
    }
  }

  @Nested
  @DisplayName("Teste nyFarskapserklaering")
  class NyFarskapserklaering {

    @Test
    @DisplayName("Mor skal kunne opprette farskapserklaering for barn med termindato")
    void morSkalKunneOppretteFarskapserklaeringForBarnMedTermindato() {

      // given
      brukeStandardMocks(MOR.getFoedselsnummer());

      // legger på redirecturl til dokument i void-metode
      doAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        var dokument = (Dokument) args[0];
        dokument.setSigneringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(REDIRECT_URL).build());
        return null;
      }).when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any(), any());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(OppretteFarskapserklaeringRequest.builder().skriftspraak(Skriftspraak.BOKMAAL).barn(mapper.toDto(BARN_UTEN_FNR))
              .opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertAll(() -> assertTrue(respons.getStatusCode().is2xxSuccessful()),
          () -> assertEquals(REDIRECT_URL, respons.getBody().getRedirectUrlForSigneringMor()));

    }

    @Test
    @DisplayName("Skal gi BAD REQUEST dersom farskapserklæring allerede eksisterer for utfødt barn med samme foreldre")
    void skalGiBadRequestDersomFarskapserklaeringAlleredeEksistererForUfoedtBarnMedSammeForeldre() {

      // given
      var farskapserklaeringSomVenterPaaFar = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      var eksisterendeFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFar);

      brukeStandardMocks(MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(
              OppretteFarskapserklaeringRequest.builder().barn(mapper.toDto(BARN_UTEN_FNR)).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR)
                  .build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());

      // cleanup db
      farskapserklaeringDao.delete(eksisterendeFarskapserklaering);
    }

    @Test
    @DisplayName("Skal skal gi BAD REQUEST dersom termindato er utenfor gyldig område")
    void skalGiBadRequestDersomTermindatoErUtenforGyldigOmraade() {

      // given
      brukeStandardMocks(MOR.getFoedselsnummer());

      var barnMedTermindatoForLangtFremITid = BarnDto.builder()
          .termindato(LocalDate.now().plusWeeks(farskapsportalApiEgenskaper.getMaksAntallUkerTilTermindato() + 1)).build();

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST, initHttpEntity(
          OppretteFarskapserklaeringRequest.builder().barn(barnMedTermindatoForLangtFremITid).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR)
              .build()), OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());
    }

    @Test
    void skalGiBadRequestDersomSkriftspraakMangler() {

      // given
      brukeStandardMocks(MOR.getFoedselsnummer());

      // legger på redirecturl til dokument i void-metode
      doAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        var dokument = (Dokument) args[0];
        dokument.setSigneringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(REDIRECT_URL).build());
        return null;
      }).when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any(), any());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(
              OppretteFarskapserklaeringRequest.builder().barn(mapper.toDto(BARN_UTEN_FNR)).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR)
                  .build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());
    }

    @Test
    void skalGiBadRequestDersomFarsFoedselsnummerMangler() {

      // given
      brukeStandardMocks(MOR.getFoedselsnummer());

      // legger på redirecturl til dokument i void-metode
      doAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        var dokument = (Dokument) args[0];
        dokument.setSigneringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(REDIRECT_URL).build());
        return null;
      }).when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any(), any());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(
              OppretteFarskapserklaeringRequest.builder().skriftspraak(Skriftspraak.BOKMAAL).barn(mapper.toDto(BARN_UTEN_FNR)).opplysningerOmFar(
                  KontrollerePersonopplysningerRequest.builder().navn(NAVN_FAR.sammensattNavn()).build()).build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());
    }

    @Test
    void skalGiBadRequestDersomFarsNavnMangler() {

      // given
      brukeStandardMocks(MOR.getFoedselsnummer());

      // legger på redirecturl til dokument i void-metode
      doAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        var dokument = (Dokument) args[0];
        dokument.setSigneringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(REDIRECT_URL).build());
        return null;
      }).when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any(), any());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(
              OppretteFarskapserklaeringRequest.builder().skriftspraak(Skriftspraak.BOKMAAL).barn(mapper.toDto(BARN_UTEN_FNR)).opplysningerOmFar(
                  KontrollerePersonopplysningerRequest.builder().foedselsnummer(FAR.getFoedselsnummer()).build()).build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());
    }

    @Test
    void skalGiBadRequestDersomBarnMangler() {

      // given
      brukeStandardMocks(MOR.getFoedselsnummer());

      // legger på redirecturl til dokument i void-metode
      doAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        var dokument = (Dokument) args[0];
        dokument.setSigneringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(REDIRECT_URL).build());
        return null;
      }).when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any(), any());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(OppretteFarskapserklaeringRequest.builder().skriftspraak(Skriftspraak.BOKMAAL).opplysningerOmFar(
                  KontrollerePersonopplysningerRequest.builder().navn(NAVN_FAR.sammensattNavn()).foedselsnummer(FAR.getFoedselsnummer()).build())
              .build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());
    }


    @Test
    @DisplayName("Skal gi BAD REQUEST dersom oppgitt nyfoedt mangler relasjon til mor")
    void skalGiBadRequestDersomOppgittNyfoedtManglerRelasjonTilMor() {

      // given
      brukeStandardMocks(MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(
              OppretteFarskapserklaeringRequest.builder().barn(mapper.toDto(BARN_MED_FNR)).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());
    }

    @Test
    @DisplayName("Mor kan opprette farskapserklæring selv om far er gift")
    void morKanOppretteFarskapserklaeringSelvOmFarErGift() {

      // given
      brukeStandardMocksUtenPdlApi(MOR.getFoedselsnummer());

      var sivilstandMor = Sivilstandtype.UGIFT;
      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(KJOENNSHISTORIKK_MOR),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonSivilstand(sivilstandMor),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0100").build())
                  .build()),
              new HentPersonDoedsfall(null),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR).build())),
          MOR.getFoedselsnummer());

      var sivilstandFar = Sivilstandtype.GIFT;
      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(KJOENNSHISTORIKK_FAR),
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonSivilstand(sivilstandFar),
              new HentPersonDoedsfall(null),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                  .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR).build())
          ),
          KONTROLLEREOPPLYSNINGER_OM_FAR.getFoedselsnummer());

      // legger på redirecturl til dokument i void-metode
      doAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        var dokument = (Dokument) args[0];
        dokument.setSigneringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(lageUrl("/redirect-mor")).build());
        return null;
      }).when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any(), any());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(OppretteFarskapserklaeringRequest.builder().skriftspraak(Skriftspraak.BOKMAAL).barn(mapper.toDto(BARN_UTEN_FNR))
              .opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertTrue(respons.getStatusCode().is2xxSuccessful());
    }

    @Test
    @DisplayName("Skal gi BAD REQUEST ved opprettelse av farskapserklæring dersom mor er gift")
    void skalGiBadRequestVedOpprettelseAvFarskapserklaeringDersomMorErGift() {

      // given
      var sivilstandMor = Sivilstandtype.GIFT;
      brukeStandardMocksUtenPdlApi(MOR.getFoedselsnummer());
      brukeStandardMocksPdlApiAngiSivilstandMor(sivilstandMor);

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(
              OppretteFarskapserklaeringRequest.builder().barn(mapper.toDto(BARN_MED_FNR)).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());
    }
  }

  @Nested
  @DisplayName("Teste oppdatereStatusEtterRedirect")
  class OppdatereStatusEtterRedirect {

    @Test
    @DisplayName("Skal oppdatere status på signeringsjobb etter mors redirect")
    void skalOppdatereStatusPaaSigneringsjobbEtterMorsRedirect() {

      // given
      var farskapserklaeringUtenSignaturer = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));

      farskapserklaeringUtenSignaturer.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(null);

      var lagretFarskapserklaering = farskapserklaeringDao.save(farskapserklaeringUtenSignaturer);
      lagretFarskapserklaering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet...".getBytes()).build());
      lagretFarskapserklaering.getDokument().setStatusUrl(lageUrl("/status"));
      farskapserklaeringDao.save(lagretFarskapserklaering);

      var registrertNavnMor = NAVN_MOR;
      var registrertNavnFar = NAVN_FAR;
      var statuslenke = lagretFarskapserklaering.getDokument().getStatusUrl();
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkMor = getKjoennshistorikk(KjoennType.KVINNE);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkMor),
              new HentPersonNavn(mapper.modelMapper(registrertNavnMor, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false)),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonNavn(mapper.modelMapper(registrertNavnFar, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false)),
          FAR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenReturn(
          DokumentStatusDto.builder()
              .bekreftelseslenke(lageUri("/confirmation"))
              .statuslenke(tilUri(statuslenke))
              .statusSignering(StatusSignering.PAAGAAR)
              .padeslenke(lageUri("/pades"))
              .signaturer(List.of(
                  SignaturDto.builder()
                      .signatureier(MOR.getFoedselsnummer())
                      .harSignert(true)
                      .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                      .xadeslenke(lageUri("/xades"))
                      .build()))
              .build());

      when(difiESignaturConsumer.henteSignertDokument(any())).thenReturn(lagretFarskapserklaering.getDokument().getDokumentinnhold().getInnhold());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(
          UriComponentsBuilder.fromHttpUrl(initHenteDokumentEtterRedirect()).queryParam("status_query_token", "Sjalalala-lala").build().encode()
              .toString(), HttpMethod.PUT, null, FarskapserklaeringDto.class);

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

      assertAll(
          () -> assertTrue(respons.getStatusCode().is2xxSuccessful()),
          () -> assertThat(respons.getBody().getDokument().getSignertAvMor()).isNotNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> AssertionsForClassTypes.assertThat(
              oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt()).isNotNull(),
          () -> AssertionsForClassTypes.assertThat(
              oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()).isNull()
      );

      // clean-up testdata
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @SneakyThrows
    @Test
    @DisplayName("Skal oppdatere status for signeringsjobb etter redirect")
    void skalOppdatereStatusForSigneringsjobbEtterRedirect() {

      // given
      var farskapserklaeringSignertAvMor = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      farskapserklaeringSignertAvMor.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      farskapserklaeringSignertAvMor.getDokument().setStatusUrl("https://esignering.no/status");
      farskapserklaeringSignertAvMor.getDokument().setPadesUrl("https://esignering.no/" + MOR.getFoedselsnummer() + "/status");
      var lagretFarskapserklaeringSignertAvMor = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSignertAvMor);

      var registrertNavnFar = NAVN_FAR;
      var registrertNavnMor = NAVN_MOR;
      var statuslenke = farskapserklaeringSignertAvMor.getDokument().getStatusUrl();
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar),
              new HentPersonNavn(mapper.modelMapper(registrertNavnFar, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false)),
          FAR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonNavn(mapper.modelMapper(registrertNavnMor, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class)),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false)),
          MOR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenReturn(
          DokumentStatusDto.builder()
              .bekreftelseslenke(lageUri("/confirmation"))
              .statuslenke(new URI(statuslenke)).statusSignering(StatusSignering.SUKSESS)
              .padeslenke(lageUri("/pades"))

              .signaturer(List.of(
                  SignaturDto.builder()
                      .signatureier(FAR.getFoedselsnummer())
                      .harSignert(true)
                      .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                      .xadeslenke(lageUri("/xades"))
                      .build())).build());

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(lagretFarskapserklaeringSignertAvMor.getDokument().getDokumentinnhold().getInnhold());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(
          UriComponentsBuilder.fromHttpUrl(initHenteDokumentEtterRedirect()).queryParam("status_query_token", "Sjalalala-lala").build().encode()
              .toString(), HttpMethod.PUT, null, FarskapserklaeringDto.class);

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaeringSignertAvMor.getId());

      assertAll(
          () -> assertTrue(respons.getStatusCode().is2xxSuccessful()),
          () -> assertThat(respons.getBody().getSendtTilSkatt()).isNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering.get().getMeldingsidSkatt()).isNotNull(),
          () -> AssertionsForClassTypes.assertThat(
              oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()).isNotNull()
      );

      // clean-up testdata
      farskapserklaeringDao.delete(lagretFarskapserklaeringSignertAvMor);
    }

    @Test
    void skalLagreOppdatertPadesUrlVedOppdateringAvStatus() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      oppgavebestillingDao.deleteAll();

      // given
      var oppdatertPades = lageUri("/pades-opppdatert");
      var farskapserklaeringSignertAvMor = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      var lagretFarskapserklaeringSignertAvMor = farskapserklaeringDao.save(farskapserklaeringSignertAvMor);
      lagretFarskapserklaeringSignertAvMor.getDokument().setStatusUrl(lageUrl("/status"));
      lagretFarskapserklaeringSignertAvMor.getDokument().setDokumentinnhold(Dokumentinnhold.builder()
          .innhold("Jeg erklærer med dette farskap til barnet...".getBytes()).build());
      farskapserklaeringDao.save(lagretFarskapserklaeringSignertAvMor);

<<<<<<< HEAD:apps/api/src/test/java/no/nav/farskapsportal/backend/apps/api/provider/rs/FarskapsportalControllerTest.java
      var registrertNavnFar = NAVN_FAR;
      var registrertNavnMor = NAVN_MOR;
=======
      var lagretOppgavebestilling = oppgavebestillingDao.save(
          Oppgavebestilling.builder().farskapserklaering(lagretFarskapserklaeringSignertAvMor).eventId(UUID.randomUUID().toString())
              .opprettet(LocalDateTime.now()).build());

      var registrertNavnFar = FAR.getNavn();
      var registrertNavnMor = MOR.getNavn();
>>>>>>> main:src/test/java/no/nav/farskapsportal/provider/rs/FarskapsportalControllerTest.java
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      doNothing().when(brukernotifikasjonConsumer).sletteFarsSigneringsoppgave(lagretOppgavebestilling.getEventId(), mapper.toEntity(FAR));
      doNothing().when(brukernotifikasjonConsumer).informereForeldreOmTilgjengeligFarskapserklaering(mapper.toEntity(FAR), mapper.toEntity(MOR));
      stsStub.runSecurityTokenServiceStub("jalla");
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar), new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(registrertNavnFar, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(registrertNavnMor, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          MOR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenReturn(
          DokumentStatusDto.builder()
              .statuslenke(lageUri("/status"))
              .bekreftelseslenke(lageUri("/confirmation"))
              .statusSignering(StatusSignering.SUKSESS)
              .padeslenke(oppdatertPades)
              .signaturer(List.of(SignaturDto.builder()
                  .signatureier(FAR.getFoedselsnummer())
                  .harSignert(true)
                  .xadeslenke(lageUri("/xades"))
                  .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                  .build()))
              .build());

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(lagretFarskapserklaeringSignertAvMor.getDokument().getDokumentinnhold().getInnhold());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(
          UriComponentsBuilder.fromHttpUrl(initHenteDokumentEtterRedirect()).queryParam("status_query_token", "Sjalalala-lala").build().encode()
              .toString(), HttpMethod.PUT, null, FarskapserklaeringDto.class);

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaeringSignertAvMor.getId());

      assertAll(
          () -> assertTrue(respons.getStatusCode().is2xxSuccessful()),
          () -> assertThat(respons.getBody().getMeldingsidSkatt()).isNotNull(),
          () -> assertThat(respons.getBody().getSendtTilSkatt()).isNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering.get().getDokument().getPadesUrl())
              .isEqualTo(oppdatertPades.toString()),
          () -> AssertionsForClassTypes.assertThat(
              oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()).isNotNull()
      );
    }

    @Test
    void skalDeaktivereFarskapserklaeringDersomMorAvbryterSignering() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      oppgavebestillingDao.deleteAll();

      // given
<<<<<<< HEAD:apps/api/src/test/java/no/nav/farskapsportal/backend/apps/api/provider/rs/FarskapsportalControllerTest.java
      var bestillingAvNyFarskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      bestillingAvNyFarskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(null);
      var nyopprettetFarskapserklaering = farskapserklaeringDao.save(bestillingAvNyFarskapserklaering);
      nyopprettetFarskapserklaering.getDokument().setStatusUrl(lageUrl("/status"));
=======
      var bestillingAvNyFarskapserklaering = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);
      var nyopprettetFarskapserklaering = farskapserklaeringDao.save(mapper.toEntity(bestillingAvNyFarskapserklaering));
      nyopprettetFarskapserklaering.getDokument().setStatusUrl(lageUrl("/status").toString());
>>>>>>> main:src/test/java/no/nav/farskapsportal/provider/rs/FarskapsportalControllerTest.java
      nyopprettetFarskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder()
          .innhold("Jeg erklærer med dette farskap til barnet...".getBytes()).build());
      farskapserklaeringDao.save(nyopprettetFarskapserklaering);

      var registrertNavnMor = NAVN_MOR;
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkMor = getKjoennshistorikk(KjoennType.KVINNE);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkMor), new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(registrertNavnMor, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          MOR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenReturn(
          DokumentStatusDto.builder()
              .statuslenke(lageUri("/status"))
              .bekreftelseslenke(lageUri("/confirmation"))
              // Mor avbryter signering => status blir feilet
              .statusSignering(StatusSignering.FEILET)
              .padeslenke(null)
              .signaturer(List.of(SignaturDto.builder()
                  .signatureier(MOR.getFoedselsnummer())
                  .harSignert(false)
                  .xadeslenke(null)
                  .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                  .build()))
              .build());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(
          UriComponentsBuilder.fromHttpUrl(initHenteDokumentEtterRedirect()).queryParam("status_query_token", "Sjalalala-lala").build().encode()
              .toString(), HttpMethod.PUT, null, FarskapserklaeringDto.class);

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(nyopprettetFarskapserklaering.getId());

      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.GONE),
          () -> assertThat(respons.getBody().getMeldingsidSkatt()).isNull(),
          () -> assertThat(respons.getBody().getSendtTilSkatt()).isNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering.get().getDeaktivert()).isNotNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering.get().getDokument().getPadesUrl()).isNull(),
          () -> AssertionsForClassTypes.assertThat(
              oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt()).isNull()
      );
    }

    @Test
    void skalDeaktivereFarskapserklaeringDersomFarAvbryterSignering() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      oppgavebestillingDao.deleteAll();

      // given
      var bestillingAvNyFarskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      var farskapserklaeringSignertAvMor = farskapserklaeringDao.save(bestillingAvNyFarskapserklaering);
      farskapserklaeringSignertAvMor.getDokument().setStatusUrl(lageUrl("/status"));
      farskapserklaeringSignertAvMor.getDokument().setDokumentinnhold(Dokumentinnhold.builder()
          .innhold("Jeg erklærer med dette farskap til barnet...".getBytes()).build());
      farskapserklaeringDao.save(farskapserklaeringSignertAvMor);

      var registrertNavnFar = NAVN_FAR;
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar), new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(registrertNavnFar, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenReturn(
          DokumentStatusDto.builder()
              .statuslenke(lageUri("/status"))
              .bekreftelseslenke(lageUri("/confirmation"))
              // Far avbryter signering => status blir feilet
              .statusSignering(StatusSignering.FEILET)
              .padeslenke(null)
              .signaturer(List.of(SignaturDto.builder()
                  .signatureier(FAR.getFoedselsnummer())
                  .harSignert(false)
                  .xadeslenke(null)
                  .tidspunktForStatus(ZonedDateTime.now().minusSeconds(3))
                  .build()))
              .build());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(
          UriComponentsBuilder.fromHttpUrl(initHenteDokumentEtterRedirect()).queryParam("status_query_token", "Sjalalala-lala").build().encode()
              .toString(), HttpMethod.PUT, null, FarskapserklaeringDto.class);

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(farskapserklaeringSignertAvMor.getId());

      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.GONE),
          () -> assertThat(respons.getBody().getMeldingsidSkatt()).isNull(),
          () -> assertThat(respons.getBody().getSendtTilSkatt()).isNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering.get().getDeaktivert()).isNotNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering.get().getDokument().getPadesUrl()).isNull(),
          () -> AssertionsForClassTypes.assertThat(
              oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()).isNull()
      );
    }

    @Test
    void skalReturnereHttpStatusNotFoundDersomStatusQueryTokenErUkjent() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      oppgavebestillingDao.deleteAll();

      // given
      var bestillingAvNyFarskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      var farskapserklaeringSignertAvMor = farskapserklaeringDao.save(bestillingAvNyFarskapserklaering);
      farskapserklaeringSignertAvMor.getDokument().setStatusUrl(lageUrl("/status"));
      farskapserklaeringSignertAvMor.getDokument().setDokumentinnhold(Dokumentinnhold.builder()
          .innhold("Jeg erklærer med dette farskap til barnet...".getBytes()).build());
      farskapserklaeringDao.save(farskapserklaeringSignertAvMor);

      var registrertNavnFar = NAVN_FAR;
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar), new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(registrertNavnFar, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenThrow(new EsigneringConsumerException(Feilkode.ESIGNERING_UKJENT_TOKEN));

      // when
      var respons = httpHeaderTestRestTemplate.exchange(
          UriComponentsBuilder.fromHttpUrl(initHenteDokumentEtterRedirect()).queryParam("status_query_token", "Sjalalala-lala").build().encode()
              .toString(), HttpMethod.PUT, null, FarskapserklaeringDto.class);

      // then
      var oppdatertFarskapserklaering = farskapserklaeringDao.findById(farskapserklaeringSignertAvMor.getId());

      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
          () -> assertThat(respons.getBody().getMeldingsidSkatt()).isNull(),
          () -> assertThat(respons.getBody().getSendtTilSkatt()).isNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering.get().getDeaktivert()).isNull(),
          () -> AssertionsForClassTypes.assertThat(oppdatertFarskapserklaering.get().getDokument().getPadesUrl()).isNull(),
          () -> AssertionsForClassTypes.assertThat(
              oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()).isNull()
      );
    }

    @Test
    void skalReturnereHttpStatusInternalServerErrorDersomXadeslenkeManglerEtterSigneringMedSuksess() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();
      oppgavebestillingDao.deleteAll();

      // given
      var farskapserklaeringSignertAvMor = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
          henteBarnUtenFnr(5));
      var lagretFarskapserklaeringSignertAvMor = farskapserklaeringDao.save(farskapserklaeringSignertAvMor);
      lagretFarskapserklaeringSignertAvMor.getDokument().setStatusUrl(lageUrl("/status"));
      lagretFarskapserklaeringSignertAvMor.getDokument().setDokumentinnhold(Dokumentinnhold.builder()
          .innhold("Jeg erklærer med dette farskap til barnet...".getBytes()).build());
      farskapserklaeringDao.save(lagretFarskapserklaeringSignertAvMor);

<<<<<<< HEAD:apps/api/src/test/java/no/nav/farskapsportal/backend/apps/api/provider/rs/FarskapsportalControllerTest.java
      var registrertNavnFar = NAVN_FAR;
      var registrertNavnMor = NAVN_MOR;
=======
      var lagretOppgavebestilling = oppgavebestillingDao.save(
          Oppgavebestilling.builder().farskapserklaering(lagretFarskapserklaeringSignertAvMor).eventId(UUID.randomUUID().toString())
              .opprettet(LocalDateTime.now()).build());

      var registrertNavnFar = FAR.getNavn();
      var registrertNavnMor = MOR.getNavn();
>>>>>>> main:src/test/java/no/nav/farskapsportal/provider/rs/FarskapsportalControllerTest.java
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      doNothing().when(brukernotifikasjonConsumer).sletteFarsSigneringsoppgave(lagretOppgavebestilling.getEventId(), mapper.toEntity(FAR));
      doNothing().when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(mapper.toEntity(FAR), mapper.toEntity(MOR));
      stsStub.runSecurityTokenServiceStub("jalla");
      LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar), new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(registrertNavnFar, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(registrertNavnMor, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          MOR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenThrow(new EsigneringConsumerException(Feilkode.ESIGNERING_MANGLENDE_DATA));

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(lagretFarskapserklaeringSignertAvMor.getDokument().getDokumentinnhold().getInnhold());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(
          UriComponentsBuilder.fromHttpUrl(initHenteDokumentEtterRedirect()).queryParam("status_query_token", "Sjalalala-lala").build().encode()
              .toString(), HttpMethod.PUT, null, FarskapserklaeringDto.class);

      // then
      assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Nested
  @DisplayName("Hente ny redirect-url")
  class HenteNyRedirectUrl {

    @Test
    void skalHenteNyRedirectUrlForFarDersomFarskapserklaeringEksisterer() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var nyRedirectUrl = lageUri("/redirect-url-far");
      var undertegnerUrlFar = lageUri("/signer-url-far");

      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));

      farskapserklaering.getDokument().getSigneringsinformasjonFar().setUndertegnerUrl(undertegnerUrlFar.toString());
      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      when(difiESignaturConsumer.henteNyRedirectUrl(undertegnerUrlFar)).thenReturn(nyRedirectUrl);

      // when
      var respons = httpHeaderTestRestTemplate.exchange(
          UriComponentsBuilder.fromHttpUrl(initHenteNyRedirectUrl()).queryParam("id_farskapserklaering", lagretFarskapserklaering.getId()).build()
              .encode().toString(), HttpMethod.POST, null, String.class);

      // then
      assertThat(nyRedirectUrl.toString()).isEqualTo(respons.getBody());
    }

    @Test
    void skalGiFeilkodeFantIkkeFarskapserklaeringVedHentingAvNyRedirectUrlDersomFarskapserklaeringIkkeFinnes() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var nyRedirectUrl = lageUri("/redirect-url-far");
      var undertegnerUrlFar = lageUri("/signer-url-far");

      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));
      farskapserklaering.getDokument().getSigneringsinformasjonFar().setUndertegnerUrl(undertegnerUrlFar.toString());
      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      when(difiESignaturConsumer.henteNyRedirectUrl(undertegnerUrlFar)).thenReturn(nyRedirectUrl);

      ResponseEntity<FarskapserklaeringFeilResponse> respons = null;

      // when
      try {
        respons = httpHeaderTestRestTemplate
            .exchange(
                UriComponentsBuilder.fromHttpUrl(initHenteNyRedirectUrl()).queryParam("id_farskapserklaering", lagretFarskapserklaering.getId() + 1)
                    .build().encode().toString(),
                HttpMethod.POST, null, FarskapserklaeringFeilResponse.class);
      } catch (Exception e) {
        e.printStackTrace();
      }

      // then
      var farskapserklaeringFeilResponse = respons.getBody();

      ResponseEntity<FarskapserklaeringFeilResponse> finalRespons = respons;
      assertAll(() -> assertThat(finalRespons.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
          () -> assertThat(Feilkode.FANT_IKKE_FARSKAPSERKLAERING).isEqualTo(farskapserklaeringFeilResponse.getFeilkode()),
          () -> assertThat(Feilkode.FANT_IKKE_FARSKAPSERKLAERING.getBeskrivelse()).isEqualTo(farskapserklaeringFeilResponse.getFeilkodebeskrivelse()),
          () -> assertThat(finalRespons.getBody().getAntallResterendeForsoek()).isEmpty());
    }
  }

  @Nested
  @DisplayName("Oppdatere farskapserklæring")
  class OppdatereFarskapserklaering {

    @BeforeEach
    void ryddeTestdata() {
      farskapserklaeringDao.deleteAll();
      statusKontrollereFarDao.deleteAll();
      forelderDao.deleteAll();
    }

    @Test
    void skalOppdatereFarBorSammenMedMor() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initOppdatereFarskapserklaering(), HttpMethod.PUT,
          initHttpEntity(
              OppdatereFarskapserklaeringRequest.builder().idFarskapserklaering(lagretFarskapserklaering.getId()).farBorSammenMedMor(true).build()),
          OppdatereFarskapserklaeringResponse.class);

      // then
      assertAll(() -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.CREATED),
          () -> assertThat(respons.getBody().getOppdatertFarskapserklaeringDto().getFarBorSammenMedMor()).isTrue());
    }

    @Test
    void skalGiBadRequestDersomMorForsoekerAaOppdatereBorSammen() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_MOR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
              new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
              new HentPersonNavn(mapper.modelMapper(NAVN_FAR, no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto.class))),
          FAR.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initOppdatereFarskapserklaering(), HttpMethod.PUT,
          initHttpEntity(
              OppdatereFarskapserklaeringRequest.builder().idFarskapserklaering(lagretFarskapserklaering.getId()).farBorSammenMedMor(false).build()),
          FarskapserklaeringFeilResponse.class);

      // then
      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
          () -> assertThat(respons.getBody().getFeilkode()).isEqualTo(Feilkode.BOR_SAMMEN_INFO_KAN_BARE_OPPDATERES_AV_FAR)
      );
    }

    @Test
    void skalGiFeilkodePersonIkkePartIFarskapserklaeringDersomPaaloggetPersonIkkeErPartIOppgittFarskapserklaering() {

      // given
      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn("12345678910");

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initOppdatereFarskapserklaering(), HttpMethod.PUT,
          initHttpEntity(
              OppdatereFarskapserklaeringRequest.builder().idFarskapserklaering(lagretFarskapserklaering.getId()).farBorSammenMedMor(true).build()),
          FarskapserklaeringFeilResponse.class);

      // then
      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
          () -> assertThat(respons.getBody().getFeilkode()).isEqualTo(Feilkode.PERSON_IKKE_PART_I_FARSKAPSERKLAERING));
    }
  }

  @Nested
  @DisplayName("Hente dokumentinnhold")
  class HenteDokumentinnhold {

    @BeforeEach
    void ryddeTestdata() {
      farskapserklaeringDao.deleteAll();
      statusKontrollereFarDao.deleteAll();
      forelderDao.deleteAll();
    }

    @Test
    void skalHenteDokumentInnholdForFarMedVentendeErklaering() {

      // given
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());

      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));
      farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder()
          .innhold("Jeg erklærer herved farskap til dette barnet".getBytes(StandardCharsets.UTF_8)).build());
      farskapserklaeringDao.save(farskapserklaering);

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initHenteDokumentinnhold(farskapserklaering.getId()), HttpMethod.GET, null, byte[].class);

      // then
      assertArrayEquals(farskapserklaering.getDokument().getDokumentinnhold().getInnhold(), respons.getBody());
    }

    @Test
    void skalGiBadRequestDersomFarIkkeErPartIErklaering() {

      // given
      var farSomIkkeErPartIErklaeringen = FOEDSELSDATO_FAR.format(DateTimeFormatter.ofPattern("ddMMyy")) + "55555";
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(farSomIkkeErPartIErklaeringen);

      var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR)
          , henteBarnUtenFnr(5));
            farskapserklaeringDao.save(farskapserklaering);

      // when
      var respons = httpHeaderTestRestTemplate
          .exchange(initHenteDokumentinnhold(farskapserklaering.getId()), HttpMethod.GET, null, FarskapserklaeringFeilResponse.class);

      // then
      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
          () -> assertThat(respons.getBody().getFeilkode()).isEqualTo(Feilkode.PERSON_IKKE_PART_I_FARSKAPSERKLAERING)
      );
    }

    @Test
    void skalGiNotFoundDersomErklaeringIkkeFinnes() {

      // given
      var idFarskapserklaeringSomIkkeFinnes = 4;

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate
          .exchange(initHenteDokumentinnhold(idFarskapserklaeringSomIkkeFinnes), HttpMethod.GET, null, FarskapserklaeringFeilResponse.class);

      // then
      assertAll(
          () -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
          () -> assertThat(respons.getBody().getFeilkode()).isEqualTo(Feilkode.FANT_IKKE_FARSKAPSERKLAERING)
      );
    }
  }
}

