package no.nav.farskapsportal.provider.rs;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.FOEDSELSDATO_FAR;
import static no.nav.farskapsportal.TestUtils.FOEDSELSDATO_MOR;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static no.nav.farskapsportal.TestUtils.henteNyligFoedtBarn;
import static no.nav.farskapsportal.TestUtils.lageUrl;
import static no.nav.farskapsportal.TestUtils.tilUri;
import static no.nav.farskapsportal.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK;
import static no.nav.farskapsportal.consumer.pdl.PdlApiConsumer.PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
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
import no.nav.farskapsportal.config.FarskapsportalConfig.OidcTokenSubjectExtractor;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.esignering.api.DokumentStatusDto;
import no.nav.farskapsportal.consumer.esignering.api.SignaturDto;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonRolle;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonerDto;
import no.nav.farskapsportal.consumer.pdl.api.FolkeregisteridentifikatorDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.BostedsadresseDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.UtenlandskAdresseDto;
import no.nav.farskapsportal.consumer.pdl.api.bostedsadresse.VegadresseDto;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonBostedsadresse;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonDoedsfall;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFamilierelasjoner;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFoedsel;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonFolkeregisteridentifikator;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonKjoenn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.consumer.pdl.stub.HentPersonSivilstand;
import no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.ValideringException;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.persistence.entity.Signeringsinformasjon;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.util.Mapper;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

@DisplayName("FarskapsportalController")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 8096)
public class FarskapsportalControllerTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final NavnDto NAVN_MOR = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final NavnDto NAVN_FAR = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
  private static final BarnDto BARN_UTEN_FNR = henteBarnUtenFnr(5);
  private static final BarnDto BARN_MED_FNR = henteNyligFoedtBarn();
  private static final KontrollerePersonopplysningerRequest KONTROLLEREOPPLYSNINGER_OM_FAR = KontrollerePersonopplysningerRequest.builder()
      .foedselsnummer(FAR.getFoedselsnummer()).navn(FAR.getFornavn() + " " + FAR.getEtternavn()).build();
  private static final Map<KjoennType, LocalDateTime> KJOENNSHISTORIKK_MOR = getKjoennshistorikk(KjoennType.KVINNE);
  private static final Map<KjoennType, LocalDateTime> KJOENNSHISTORIKK_FAR = getKjoennshistorikk(KjoennType.MANN);
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
  @MockBean
  private SkattConsumer skattConsumer;
  @Autowired
  private PersistenceService persistenceService;
  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;
  @Autowired
  private ForelderDao forelderDao;
  @Autowired
  private FarskapsportalEgenskaper farskapsportalEgenskaper;
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

  private static Map<KjoennType, LocalDateTime> getKjoennshistorikk(KjoennType kjoennType) {
    return Stream.of(new Object[][]{{kjoennType, LocalDateTime.now()}})
        .collect(Collectors.toMap(data -> (KjoennType) data[0], data -> (LocalDateTime) data[1]));
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
            new HentPersonNavn(NAVN_MOR),
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
            new HentPersonNavn(NAVN_FAR),
            new HentPersonDoedsfall(null),
            new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK)
                .type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR).build())),
        KONTROLLEREOPPLYSNINGER_OM_FAR.getFoedselsnummer());
  }

  private void brukeStandardMocksUtenPdlApi(String fnrPaaloggetBruker) {

    stsStub.runSecurityTokenServiceStub("jalla");

    when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(fnrPaaloggetBruker);

    when(pdfGeneratorConsumer.genererePdf(any(), any(), any())).thenReturn("Jeg erklærer med dette farskap til barnet..".getBytes());
    doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());
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

    @Test
    @DisplayName("Skal liste nylig fødte barn uten registrert far ved henting av brukerinformasjon for mor")
    void skalListeNyligFoedteBarnUtenRegistrertFarVedHentingAvBrukerinformasjonForMor() {

      // given
      var foedselsdatoSpedbarn = LocalDate.now().minusMonths(2).minusDays(13);
      var fnrSpedbarn = foedselsdatoSpedbarn.format(DateTimeFormatter.ofPattern("ddMMyy")) + "00000";
      var foedselsdatoMor = foedselsdatoSpedbarn.minusYears(28).minusMonths(2).minusDays(13);
      var fnrMor = foedselsdatoMor.format(DateTimeFormatter.ofPattern("ddMMyy")) + "12340";

      Map<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);
      stsStub.runSecurityTokenServiceStub("jalla");
      var morsRelasjonTilBarn = FamilierelasjonerDto.builder().minRolleForPerson(FamilierelasjonRolle.MOR)
          .relatertPersonsRolle(FamilierelasjonRolle.BARN).relatertPersonsIdent(fnrSpedbarn).build();
      var spedbarnetsRelasjonTilMor = FamilierelasjonerDto.builder().relatertPersonsRolle(FamilierelasjonRolle.MOR).relatertPersonsIdent(fnrMor)
          .minRolleForPerson(FamilierelasjonRolle.BARN).build();

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonFamilierelasjoner(morsRelasjonTilBarn, "123"),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonNavn(NAVN_MOR),
              new HentPersonBostedsadresse(BostedsadresseDto.builder()
                  .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
                  .build()),
              new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
                  .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          fnrMor);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonFamilierelasjoner(spedbarnetsRelasjonTilMor, "000"),
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

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaeringSomVenterPaaFar = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);
      farskapserklaeringSomVenterPaaFar.getDokument().setSignertAvMor(LocalDateTime.now().minusDays(3));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFar);
      lagretFarskapserklaering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      persistenceService.oppdatereFarskapserklaering(lagretFarskapserklaering);

      Map<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFamilierelasjoner(null, null),
          new HentPersonSivilstand(Sivilstandtype.UGIFT),
          new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
          new HentPersonNavn(NAVN_MOR),
          new HentPersonKjoenn(kjoennshistorikk),
          new HentPersonBostedsadresse(BostedsadresseDto.builder()
              .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
              .build()),
          new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
              .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
          new HentPersonNavn(NAVN_FAR)),
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
          () -> assertNull(brukerinformasjonResponse.getAvventerSigneringMotpart().iterator().next().getDokument().getSignertAvFar(),
              "Far har ikke signert farskapserklæringen"), () -> assertEquals(FAR.getFoedselsnummer(),
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

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaeringSomVenterPaaMor = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaMor);
      lagretFarskapserklaering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      persistenceService.oppdatereFarskapserklaering(lagretFarskapserklaering);

      Map<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFamilierelasjoner(null, null),
          new HentPersonSivilstand(Sivilstandtype.UGIFT),
          new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
          new HentPersonNavn(NAVN_MOR),
          new HentPersonKjoenn(kjoennshistorikk),
          new HentPersonBostedsadresse(BostedsadresseDto.builder()
              .vegadresse(VegadresseDto.builder().adressenavn("Stortingsgaten").husnummer("10").husbokstav("B").postnummer("0010").build())
              .build()),
          new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
              .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
          new HentPersonNavn(NAVN_FAR)),
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
          () -> assertEquals(FAR.getFoedselsnummer(),
              brukerinformasjonResponse.getAvventerSigneringBruker().iterator().next().getFar().getFoedselsnummer()),
          () -> assertEquals(0, brukerinformasjonResponse.getAvventerSigneringMotpart().size()),
          () -> assertEquals(0, brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar().size()));

      // cleanup db
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @Test
    @DisplayName("Skal liste farskapserklæringer som venter på far ved henting av brukerinformasjon for far")
    void skalListeFarskapserklaeringerSomVenterPaaFarVedHentingAvBrukerinformasjonForFar() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaeringSomVenterPaaFar = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);
      farskapserklaeringSomVenterPaaFar.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(10));

      var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFar);
      farskapserklaering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      persistenceService.oppdatereFarskapserklaering(farskapserklaering);

      Map<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.MANN);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFamilierelasjoner(null, null),
          new HentPersonSivilstand(Sivilstandtype.UGIFT),
          new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
          new HentPersonKjoenn(kjoennshistorikk),
          new HentPersonNavn(NAVN_FAR)),
          FAR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFamilierelasjoner(null, null),
          new HentPersonSivilstand(Sivilstandtype.UGIFT),
          new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
          new HentPersonNavn(NAVN_MOR)),
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
          () -> assertEquals(1, brukerinformasjonResponse.getAvventerSigneringBruker().size()), () -> assertEquals(FAR.getFoedselsnummer(),
              brukerinformasjonResponse.getAvventerSigneringBruker().iterator().next().getFar().getFoedselsnummer(),
              "Farskapserklæringen gjelder riktig far"),
          () -> assertEquals(0, brukerinformasjonResponse.getFnrNyligFoedteBarnUtenRegistrertFar().size()));

      // cleanup db
      farskapserklaeringDao.deleteAll();
    }

    @Test
    @DisplayName("Farskapserklæringer som venter på mor skal ikke dukke opp i fars liste")
    void skalIkkeListeFarskapserklaeringerSomVenterPaaMorVedHentingAvBrukerinformasjonForFar() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaeringSomVenterPaaMor = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);
      farskapserklaeringSomVenterPaaMor.getDokument().setSignertAvMor(null);
      farskapserklaeringSomVenterPaaMor.getDokument().setSignertAvFar(null);

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaMor);

      Map<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.MANN);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonFamilierelasjoner(null, null),
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonSivilstand(Sivilstandtype.UGIFT),
              new HentPersonNavn(NAVN_FAR)), FAR.getFoedselsnummer());

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

      var kjoennshistorikk = new HashMap<KjoennType, LocalDateTime>();
      stsStub.runSecurityTokenServiceStub("jalla");

      var spedbarnetsRelasjonTilMor = FamilierelasjonerDto.builder().relatertPersonsRolle(FamilierelasjonRolle.MOR).relatertPersonsIdent(fnrMor)
          .minRolleForPerson(FamilierelasjonRolle.BARN).build();

      pdlApiStub.runPdlApiHentPersonStub(List.of(new HentPersonFamilierelasjoner(null, ""), new HentPersonKjoenn(kjoennshistorikk)), fnrMor);
      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonFamilierelasjoner(spedbarnetsRelasjonTilMor, "000"), new HentPersonFoedsel(foedselsdatoSpedbarn, false)),
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
      Map<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFamilierelasjoner(null, null),
          new HentPersonSivilstand(Sivilstandtype.UGIFT),
          new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
          new HentPersonNavn(NAVN_MOR),
          new HentPersonKjoenn(kjoennshistorikk),
          new HentPersonBostedsadresse(BostedsadresseDto.builder()
              .utenlandskAdresse(UtenlandskAdresseDto.builder().adressenavnNummer("Parkway Avenue 123").bySted("Newcastle").landkode("US").build())
              .build()),
          new HentPersonFolkeregisteridentifikator(FolkeregisteridentifikatorDto.builder().type(PDL_FOLKEREGISTERIDENTIFIKATOR_TYPE_FNR)
              .status(PDL_FOLKEREGISTERIDENTIFIKATOR_STATUS_I_BRUK).build())),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
          new HentPersonNavn(NAVN_FAR)),
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
      farskapserklaeringDao.deleteAll();
      brukeStandardMocks(FAR.getFoedselsnummer());

      var signertFarskapserklaering = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);
      signertFarskapserklaering.getDokument().setSignertAvMor(LocalDateTime.now());
      signertFarskapserklaering.getDokument().setSignertAvFar(LocalDateTime.now());

      var fe = persistenceService.lagreNyFarskapserklaering(signertFarskapserklaering);
      fe.getDokument().setDokumentinnhold(
          Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet".getBytes(StandardCharsets.UTF_8)).build());
      persistenceService.oppdatereFarskapserklaering(fe);

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
      farskapserklaeringDao.deleteAll();
      brukeStandardMocks(FAR.getFoedselsnummer());
    }
  }

  @Nested
  @DisplayName("Teste kontrollereOpplysningerFar")
  class KontrollereOpplysningerFar {

    @Test
    @DisplayName("Skal gi Ok dersom navn og kjønn er riktig")
    void skalGiOkDersomNavnOgKjoennErRiktig() {

      // given
      var fnrFar = "01057244444";
      var fornavnFar = "Borat";
      var etternavnFar = "Sagidiyev";
      var registrertNavn = NavnDto.builder().fornavn(fornavnFar).etternavn(etternavnFar).build();
      stsStub.runSecurityTokenServiceStub("jalla");

      Map<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);
      Map<KjoennType, LocalDateTime> kjoennshistorikkMor = getKjoennshistorikk(KjoennType.KVINNE);
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikkFar),
              new HentPersonNavn(registrertNavn),
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
      var oppgittNavn = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");

      Map<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.KVINNE);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonNavn(oppgittNavn),
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
      var registrertNavn = NavnDto.builder().fornavn("Borat").etternavn("Sagdiyev").build();

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      stsStub.runSecurityTokenServiceStub("jalla");

      Map<KjoennType, LocalDateTime> kjoennshistorikk = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(kjoennshistorikk),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonNavn(registrertNavn),
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

    }
  }

  @Nested
  @DisplayName("Teste nyFarskapserklaering")
  class NyFarskapserklaering {

    @Test
    @DisplayName("Mor skal kunne opprette farskapserklaering for barn med termindato")
    void morSkalKunneOppretteFarskapserklaeringForBarnMedTermindato() {

      // given
      farskapserklaeringDao.deleteAll();
      brukeStandardMocks(MOR.getFoedselsnummer());

      // legger på redirecturl til dokument i void-metode
      doAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        var dokument = (Dokument) args[0];
        dokument.setSigneringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(REDIRECT_URL).build());
        return null;
      }).when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(OppretteFarskapserklaeringRequest.builder().barn(BARN_UTEN_FNR).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertAll(() -> assertTrue(respons.getStatusCode().is2xxSuccessful()),
          () -> assertEquals(REDIRECT_URL, respons.getBody().getRedirectUrlForSigneringMor()));

      // rydde testdata
      farskapserklaeringDao.deleteAll();
    }

    @Test
    @DisplayName("Skal gi BAD REQUEST dersom farskapserklæring allerede eksisterer for utfødt barn med samme foreldre")
    void skalGiBadRequestDersomFarskapserklaeringAlleredeEksistererForUfoedtBarnMedSammeForeldre() {

      // given
      farskapserklaeringDao.deleteAll();

      var farskapserklaeringSomVenterPaaFar = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);
      farskapserklaeringSomVenterPaaFar.getDokument().setSignertAvMor(LocalDateTime.now().minusDays(3));
      var eksisterendeFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFar);

      brukeStandardMocks(MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(OppretteFarskapserklaeringRequest.builder().barn(BARN_UTEN_FNR).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
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
          .termindato(LocalDate.now().plusWeeks(farskapsportalEgenskaper.getMaksAntallUkerTilTermindato() + 1)).build();

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST, initHttpEntity(
          OppretteFarskapserklaeringRequest.builder().barn(barnMedTermindatoForLangtFremITid).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR)
              .build()), OppretteFarskapserklaeringResponse.class);

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
          initHttpEntity(OppretteFarskapserklaeringRequest.builder().barn(BARN_MED_FNR).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
          OppretteFarskapserklaeringResponse.class);

      // then
      assertEquals(HttpStatus.BAD_REQUEST.value(), respons.getStatusCodeValue());
    }

    @Test
    @DisplayName("Mor kan opprette farskapserklæring selv om far er gift")
    void morKanOppretteFarskapserklaeringSelvOmFarErGift() {

      // given
      farskapserklaeringDao.deleteAll();
      brukeStandardMocksUtenPdlApi(MOR.getFoedselsnummer());

      var sivilstandMor = Sivilstandtype.UGIFT;
      pdlApiStub.runPdlApiHentPersonStub(
          List.of(
              new HentPersonKjoenn(KJOENNSHISTORIKK_MOR),
              new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
              new HentPersonSivilstand(sivilstandMor),
              new HentPersonNavn(NAVN_MOR),
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
              new HentPersonNavn(NAVN_FAR),
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
        dokument.setSigneringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(lageUrl("/redirect-mor").toString()).build());
        return null;
      }).when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initNyFarskapserklaering(), HttpMethod.POST,
          initHttpEntity(OppretteFarskapserklaeringRequest.builder().barn(BARN_UTEN_FNR).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
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
          initHttpEntity(OppretteFarskapserklaeringRequest.builder().barn(BARN_MED_FNR).opplysningerOmFar(KONTROLLEREOPPLYSNINGER_OM_FAR).build()),
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

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // given
      var farskapserklaeringUtenSignaturer = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);

      assertAll(() -> assertNull(farskapserklaeringUtenSignaturer.getDokument().getSignertAvMor()),
          () -> assertNull(farskapserklaeringUtenSignaturer.getDokument().getSignertAvFar()));

      var lagretFarskapserklaering = farskapserklaeringDao.save(mapper.toEntity(farskapserklaeringUtenSignaturer));
      lagretFarskapserklaering.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet...".getBytes()).build());
      lagretFarskapserklaering.getDokument().setDokumentStatusUrl(lageUrl("/status").toString());
      farskapserklaeringDao.save(lagretFarskapserklaering);

      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var statuslenke = lagretFarskapserklaering.getDokument().getDokumentStatusUrl();
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");
      Map<KjoennType, LocalDateTime> kjoennshistorikkMor = getKjoennshistorikk(KjoennType.KVINNE);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkMor), new HentPersonNavn(registrertNavnMor), new HentPersonFoedsel(FOEDSELSDATO_FAR, false)),
          MOR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenReturn(
          DokumentStatusDto.builder()
              .bekreftelseslenke(lageUrl("/confirmation"))
              .statuslenke(tilUri(statuslenke))
              .erSigneringsjobbenFerdig(true)
              .padeslenke(lageUrl("/pades"))
              .signaturer(List.of(
                  SignaturDto.builder()
                      .signatureier(MOR.getFoedselsnummer())
                      .harSignert(true)
                      .tidspunktForStatus(LocalDateTime.now().minusSeconds(3))
                      .xadeslenke(lageUrl("/xades"))
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
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt()).isNotNull(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()).isNull()
      );

      // clean-up testdata
      farskapserklaeringDao.delete(lagretFarskapserklaering);
    }

    @SneakyThrows
    @Test
    @DisplayName("Skal oppdatere status for signeringsjobb etter redirect")
    void skalOppdatereStatusForSigneringsjobbEtterRedirect() {

      // Rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // given
      var farskapserklaeringSignertAvMor = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);

      farskapserklaeringSignertAvMor.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(10));

      var lagretFarskapserklaeringSignertAvMor = farskapserklaeringDao.save(mapper.toEntity(farskapserklaeringSignertAvMor));
      lagretFarskapserklaeringSignertAvMor.getDokument()
          .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
      lagretFarskapserklaeringSignertAvMor.getDokument().setDokumentStatusUrl("https://esignering.no/status");
      lagretFarskapserklaeringSignertAvMor.getDokument().setPadesUrl("https://esignering.no/" + MOR.getFoedselsnummer() + "/status");
      farskapserklaeringDao.save(lagretFarskapserklaeringSignertAvMor);

      var registrertNavnFar = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      var statuslenke = lagretFarskapserklaeringSignertAvMor.getDokument().getDokumentStatusUrl();
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      doNothing().when(skattConsumer).registrereFarskap(lagretFarskapserklaeringSignertAvMor);
      stsStub.runSecurityTokenServiceStub("jalla");
      Map<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar), new HentPersonNavn(registrertNavnFar), new HentPersonFoedsel(FOEDSELSDATO_FAR, false)),
          FAR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonNavn(registrertNavnMor), new HentPersonFoedsel(FOEDSELSDATO_MOR, false)),
          MOR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenReturn(
          DokumentStatusDto.builder()
              .bekreftelseslenke(lageUrl("/confirmation"))
              .statuslenke(new URI(statuslenke)).erSigneringsjobbenFerdig(true)
              .padeslenke(lageUrl("/pades"))

              .signaturer(List.of(
                  SignaturDto.builder()
                      .signatureier(FAR.getFoedselsnummer())
                      .harSignert(true)
                      .tidspunktForStatus(LocalDateTime.now().minusSeconds(3))
                      .xadeslenke(lageUrl("/xades"))
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
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering.get().getMeldingsidSkatt()).isNotNull(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()).isNotNull()
      );

      // clean-up testdata
      farskapserklaeringDao.delete(lagretFarskapserklaeringSignertAvMor);
    }

    @Test
    @DisplayName("SkaLagreOppdatertPadesUrlVedOppdateringAvStatus")
    void skalLagreOppdatertPadesUrlVedOppdateringAvStatus() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // given
      var oppdatertPades = lageUrl("/pades-opppdatert");
      var farskapserklaeringSignertAvMor = henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR);
      farskapserklaeringSignertAvMor.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(10));
      var lagretFarskapserklaeringSignertAvMor = farskapserklaeringDao.save(mapper.toEntity(farskapserklaeringSignertAvMor));
      lagretFarskapserklaeringSignertAvMor.getDokument().setDokumentStatusUrl(lageUrl("/status").toString());
      lagretFarskapserklaeringSignertAvMor.getDokument().setDokumentinnhold(Dokumentinnhold.builder()
          .innhold("Jeg erklærer med dette farskap til barnet...".getBytes()).build());
      farskapserklaeringDao.save(lagretFarskapserklaeringSignertAvMor);

      var registrertNavnFar = NavnDto.builder().fornavn(FAR.getFornavn()).etternavn(FAR.getEtternavn()).build();
      var registrertNavnMor = NavnDto.builder().fornavn(MOR.getFornavn()).etternavn(MOR.getEtternavn()).build();
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      doNothing().when(brukernotifikasjonConsumer).sletteFarsSigneringsoppgave(lagretFarskapserklaeringSignertAvMor.getId(), FAR.getFoedselsnummer());
      doNothing().when(brukernotifikasjonConsumer)
          .informereForeldreOmTilgjengeligFarskapserklaering(FAR.getFoedselsnummer(), MOR.getFoedselsnummer());
      stsStub.runSecurityTokenServiceStub("jalla");
      Map<KjoennType, LocalDateTime> kjoennshistorikkFar = getKjoennshistorikk(KjoennType.MANN);

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonKjoenn(kjoennshistorikkFar), new HentPersonFoedsel(FOEDSELSDATO_FAR, false), new HentPersonNavn(registrertNavnFar)),
          FAR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(
          List.of(new HentPersonFoedsel(FOEDSELSDATO_MOR, false), new HentPersonNavn(registrertNavnMor)),
          MOR.getFoedselsnummer());

      when(difiESignaturConsumer.henteStatus(any(), any())).thenReturn(
          DokumentStatusDto.builder()
              .statuslenke(lageUrl("/status"))
              .bekreftelseslenke(lageUrl("/confirmation"))
              .erSigneringsjobbenFerdig(true)
              .padeslenke(oppdatertPades)
              .signaturer(List.of(SignaturDto.builder()
                  .signatureier(FAR.getFoedselsnummer())
                  .harSignert(true)
                  .xadeslenke(lageUrl("/xades"))
                  .tidspunktForStatus(LocalDateTime.now().minusSeconds(3))
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
          () -> assertThat(oppdatertFarskapserklaering).isPresent(),
          () -> assertThat(oppdatertFarskapserklaering.get().getDokument().getPadesUrl()).isEqualTo(oppdatertPades.toString()),
          () -> assertThat(oppdatertFarskapserklaering.get().getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt()).isNotNull()
      );
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
      var nyRedirectUrl = lageUrl("/redirect-url-far");
      var undertegnerUrlFar = lageUrl("/signer-url-far");

      var farskapserklaering = mapper.toEntity(henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR));

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
      var nyRedirectUrl = lageUrl("/redirect-url-far");
      var undertegnerUrlFar = lageUrl("/signer-url-far");

      var farskapserklaering = mapper.toEntity(henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR));
      farskapserklaering.getDokument().getSigneringsinformasjonFar().setUndertegnerUrl(undertegnerUrlFar.toString());
      persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());
      when(difiESignaturConsumer.henteNyRedirectUrl(undertegnerUrlFar)).thenReturn(nyRedirectUrl);

      // when
      var respons = httpHeaderTestRestTemplate
          .exchange(UriComponentsBuilder.fromHttpUrl(initHenteNyRedirectUrl()).queryParam("id_farskapserklaering", 5).build().encode().toString(),
              HttpMethod.POST, null, FarskapserklaeringFeilResponse.class);

      // then
      var farskapserklaeringFeilResponse = respons.getBody();

      assertAll(() -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
          () -> assertThat(Feilkode.FANT_IKKE_FARSKAPSERKLAERING).isEqualTo(farskapserklaeringFeilResponse.getFeilkode()),
          () -> assertThat(Feilkode.FANT_IKKE_FARSKAPSERKLAERING.getBeskrivelse()).isEqualTo(farskapserklaeringFeilResponse.getFeilkodebeskrivelse()),
          () -> assertThat(respons.getBody().getAntallResterendeForsoek()).isEmpty());
    }
  }

  @Nested
  @DisplayName("Oppdatere farskapserklæring")
  class OppdatereFarskapserklaering {

    @Test
    void skalOppdatereFarBorSammenMedMor() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering = mapper.toEntity(henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
          new HentPersonNavn(NAVN_MOR)),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
          new HentPersonNavn(NAVN_FAR)),
          FAR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initOppdatereFarskapserklaering(), HttpMethod.PUT,
          initHttpEntity(OppdatereFarskapserklaeringRequest.builder().idFarskapserklaering(lagretFarskapserklaering.getId()).borSammen(true).build()),
          OppdatereFarskapserklaeringResponse.class);

      // then
      assertAll(() -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.CREATED),
          () -> assertThat(respons.getBody().getOppdatertFarskapserklaeringDto().getFarBorSammenMedMor()).isTrue(),
          () -> assertThat(respons.getBody().getOppdatertFarskapserklaeringDto().getMorBorSammenMedFar()).isNull());
    }

    @Test
    void skalOppdatereMorBorSammenMedFar() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering = mapper.toEntity(henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      stsStub.runSecurityTokenServiceStub("jalla");

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFoedsel(FOEDSELSDATO_MOR, false),
          new HentPersonNavn(NAVN_MOR)),
          MOR.getFoedselsnummer());

      pdlApiStub.runPdlApiHentPersonStub(List.of(
          new HentPersonFoedsel(FOEDSELSDATO_FAR, false),
          new HentPersonNavn(NAVN_FAR)),
          FAR.getFoedselsnummer());

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(MOR.getFoedselsnummer());

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initOppdatereFarskapserklaering(), HttpMethod.PUT,
          initHttpEntity(
              OppdatereFarskapserklaeringRequest.builder().idFarskapserklaering(lagretFarskapserklaering.getId()).borSammen(false).build()),
          OppdatereFarskapserklaeringResponse.class);

      // then
      assertAll(() -> assertThat(respons.getStatusCode()).isEqualTo(HttpStatus.CREATED),
          () -> assertThat(respons.getBody().getOppdatertFarskapserklaeringDto().getMorBorSammenMedFar()).isFalse(),
          () -> assertThat(respons.getBody().getOppdatertFarskapserklaeringDto().getFarBorSammenMedMor()).isNull());
    }

    @Test
    void skalGiFeilkodePersonIkkePartIFarskapserklaeringDersomPaaloggetPersonIkkeErPartIOppgittFarskapserklaering() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      var farskapserklaering = mapper.toEntity(henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR));

      var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);

      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn("12345678910");

      // when
      var respons = httpHeaderTestRestTemplate.exchange(initOppdatereFarskapserklaering(), HttpMethod.PUT,
          initHttpEntity(OppdatereFarskapserklaeringRequest.builder().idFarskapserklaering(lagretFarskapserklaering.getId()).borSammen(true).build()),
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

    @Test
    void skalHenteDokumentInnholdForFarMedVentendeErklaering() {

      // rydde testdata
      farskapserklaeringDao.deleteAll();

      // given
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(FAR.getFoedselsnummer());

      var farskapserklaering = mapper.toEntity(henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR));
      farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusDays(2));
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

      // rydde testdata
      farskapserklaeringDao.deleteAll();
      forelderDao.deleteAll();

      // given
      var farSomIkkeErPartIErklaeringen = FOEDSELSDATO_FAR.format(DateTimeFormatter.ofPattern("ddMMyy")) + "55555";
      when(oidcTokenSubjectExtractor.hentPaaloggetPerson()).thenReturn(farSomIkkeErPartIErklaeringen);

      var farskapserklaering = mapper.toEntity(henteFarskapserklaeringDto(MOR, FAR, BARN_UTEN_FNR));
      farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusDays(2));
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

      // rydde testdata
      farskapserklaeringDao.deleteAll();

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

