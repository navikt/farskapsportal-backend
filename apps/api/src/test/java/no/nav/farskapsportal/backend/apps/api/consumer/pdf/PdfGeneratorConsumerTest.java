package no.nav.farskapsportal.backend.apps.api.consumer.pdf;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_NYFOEDT_BARN;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.NAVN_FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.NAVN_MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteNyligFoedtBarn;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.model.Skriftspraak;
import no.nav.farskapsportal.backend.apps.api.service.Mapper;
import no.nav.farskapsportal.backend.apps.api.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.dto.ForelderDto;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PdfGeneratorConsumerTest")
@ActiveProfiles(PROFILE_TEST)
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@SpringBootTest(classes = FarskapsportalApiApplicationLocal.class)
public class PdfGeneratorConsumerTest {

  private static final Forelder MOR = henteForelder(Forelderrolle.MOR);
  private static final Forelder FAR = henteForelder(Forelderrolle.FAR);
  private static final Barn UFOEDT_BARN = henteBarnUtenFnr(17);
  private static final Barn NYFOEDT_BARN = henteNyligFoedtBarn();
  private static boolean skriveUtPdf = true;

  @Autowired private PdfGeneratorConsumer pdfGeneratorConsumer;

  @Autowired private Mapper mapper;

  @MockBean private PersonopplysningService personopplysningServiceMock;

  @Test
  void skalGenererePdfPaaBokmaalForUfoedt() throws IOException {

    // given
    var skriftsspraak = Skriftspraak.BOKMAAL;
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var ufoedtBarn = mapper.toDto(UFOEDT_BARN);
    var mor = mapper.toDto(MOR);
    mor.setForelderrolle(Forelderrolle.MOR);

    var far = mapper.toDto(FAR);
    far.setForelderrolle(Forelderrolle.FAR);

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(ufoedtBarn, mor, far, skriftsspraak);

    // then
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }

    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Fødested"),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Termindato: "
                        + UFOEDT_BARN
                            .getTermindato()
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));

    validereInformasjonOmForeldrenePaaNorsk(dokumenttekst, mor, far, skriftsspraak);
  }

  @Test
  void skalGenererePdfPaaNynorskForUfoedt() throws IOException {

    // given
    var skriftsspraak = Skriftspraak.NYNORSK;
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var ufoedtBarn = mapper.toDto(UFOEDT_BARN);
    var mor = mapper.toDto(MOR);
    mor.setForelderrolle(Forelderrolle.MOR);

    var far = mapper.toDto(FAR);
    far.setForelderrolle(Forelderrolle.FAR);

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(ufoedtBarn, mor, far, skriftsspraak);

    // then
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }

    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Fødestad"),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Termindato: "
                        + UFOEDT_BARN
                            .getTermindato()
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));

    validereInformasjonOmForeldrenePaaNorsk(dokumenttekst, mor, far, skriftsspraak);
  }

  @Test
  void skalGenererePdfPaaEngelskForUfoedt() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var ufoedtBarn = mapper.toDto(UFOEDT_BARN);
    var mor = mapper.toDto(MOR);
    mor.setForelderrolle(Forelderrolle.MOR);

    var far = mapper.toDto(FAR);
    far.setForelderrolle(Forelderrolle.FAR);

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(ufoedtBarn, mor, far, Skriftspraak.ENGELSK);

    // then
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }

    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Place of birth"),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Expected date of birth: "
                        + UFOEDT_BARN
                            .getTermindato()
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));

    validereInformasjonOmForeldrenePaaEngelsk(dokumenttekst, mor, far);
  }

  @Test
  void skalGenererePdfPaaBokmaalForNyfoedt() throws IOException {

    // given
    var skriftspraak = Skriftspraak.BOKMAAL;
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var nyfoedtBarn = mapper.toDto(NYFOEDT_BARN);
    nyfoedtBarn.setFoedested("Stange");
    nyfoedtBarn.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);

    var mor = mapper.toDto(MOR);
    mor.setForelderrolle(Forelderrolle.MOR);

    var far = mapper.toDto(FAR);
    far.setForelderrolle(Forelderrolle.FAR);

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, far, skriftspraak);

    // then
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }

    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Termindato"),
        () -> assertThat(dokumenttekst).contains("Opplysninger om barnet"),
        () ->
            assertThat(dokumenttekst)
                .contains("Fødselsnummer: " + NYFOEDT_BARN.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Fødselsdato: "
                        + FOEDSELSDATO_NYFOEDT_BARN.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Fødested: " + nyfoedtBarn.getFoedested()));

    validereInformasjonOmForeldrenePaaNorsk(dokumenttekst, mor, far, skriftspraak);
  }

  @Test
  void skalGenererePdfPaaNynorskForNyfoedt() throws IOException {

    // given
    var skriftsspraak = Skriftspraak.NYNORSK;
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var nyfoedtBarn = mapper.toDto(NYFOEDT_BARN);
    nyfoedtBarn.setFoedested("Stange");
    nyfoedtBarn.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);

    var mor = mapper.toDto(MOR);
    mor.setForelderrolle(Forelderrolle.MOR);

    var far = mapper.toDto(FAR);
    far.setForelderrolle(Forelderrolle.FAR);

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, far, skriftsspraak);

    // then
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }

    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Termindato"),
        () -> assertThat(dokumenttekst).contains("Opplysningar om barnet"),
        () ->
            assertThat(dokumenttekst)
                .contains("Fødselsnummer: " + NYFOEDT_BARN.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Fødselsdato: "
                        + FOEDSELSDATO_NYFOEDT_BARN.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Fødestad: " + nyfoedtBarn.getFoedested()),
        () -> assertThat(dokumenttekst).doesNotContain(" fra "),
        () -> assertThat(dokumenttekst).doesNotContain(" ønsker "),
        () -> assertThat(dokumenttekst).doesNotContain(" ikke "),
        () -> assertThat(dokumenttekst).doesNotContain(" hvis "),
        () -> assertThat(dokumenttekst).doesNotContain(" foreldrene "),
        () -> assertThat(dokumenttekst).doesNotContain("barnene"),
        () -> assertThat(dokumenttekst).doesNotContain("navn"));

    validereInformasjonOmForeldrenePaaNorsk(dokumenttekst, mor, far, skriftsspraak);
  }

  @Test
  void skalGenererePdfPaaEngelskForNyfoedt() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var nyfoedtBarn = mapper.toDto(NYFOEDT_BARN);
    nyfoedtBarn.setFoedested("Stange");
    nyfoedtBarn.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);
    var mor = mapper.toDto(MOR);
    mor.setForelderrolle(Forelderrolle.MOR);

    var far = mapper.toDto(FAR);
    far.setForelderrolle(Forelderrolle.FAR);

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, far, Skriftspraak.ENGELSK);

    // then
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }

    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Expected date of birth "),
        () -> assertThat(dokumenttekst).contains("Child"),
        () ->
            assertThat(dokumenttekst)
                .contains("Social security number: " + NYFOEDT_BARN.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Date of birth: "
                        + FOEDSELSDATO_NYFOEDT_BARN.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Place of birth: " + nyfoedtBarn.getFoedested()));

    validereInformasjonOmForeldrenePaaEngelsk(dokumenttekst, mor, far);
  }

  @Test
  void skalGenererePdfPaaBokmaalForForelderMedMellomnavn() throws IOException {

    // given
    var skriftsspraak = Skriftspraak.BOKMAAL;
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var nyfoedtBarn = mapper.toDto(NYFOEDT_BARN);
    nyfoedtBarn.setFoedested("Ås");
    nyfoedtBarn.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);
    var mor = mapper.toDto(MOR);
    var farMedMellomnavn = mapper.toDto(henteForelder(Forelderrolle.FAR));
    var farsOpprinneligeNavn = NAVN_FAR;

    farMedMellomnavn.setNavn(
        NavnDto.builder()
            .fornavn(farsOpprinneligeNavn.getFornavn())
            .mellomnavn("Strømstad")
            .etternavn(farsOpprinneligeNavn.getEtternavn())
            .build());

    // when
    var pdfstroem =
        pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, farMedMellomnavn, skriftsspraak);

    // then
    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Termindato"),
        () -> assertThat(dokumenttekst).contains("Opplysninger om barnet"),
        () ->
            assertThat(dokumenttekst)
                .contains("Fødselsnummer: " + NYFOEDT_BARN.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Fødselsdato: "
                        + FOEDSELSDATO_NYFOEDT_BARN.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Fødested: " + nyfoedtBarn.getFoedested()));

    validereInformasjonOmForeldrenePaaNorsk(dokumenttekst, mor, farMedMellomnavn, skriftsspraak);

    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }
  }

  @Test
  void skalGenererePdfPaaNynorskForForelderMedMellomnavn() throws IOException {

    // given
    var skriftsspraak = Skriftspraak.NYNORSK;
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var nyfoedtBarn = mapper.toDto(NYFOEDT_BARN);
    nyfoedtBarn.setFoedested("Ås");
    nyfoedtBarn.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);
    var mor = mapper.toDto(MOR);
    var farMedMellomnavn = mapper.toDto(henteForelder(Forelderrolle.FAR));
    var farsOpprinneligeNavn = NAVN_FAR;

    farMedMellomnavn.setNavn(
        NavnDto.builder()
            .fornavn(farsOpprinneligeNavn.getFornavn())
            .mellomnavn("Strømstad")
            .etternavn(farsOpprinneligeNavn.getEtternavn())
            .build());

    // when
    var pdfstroem =
        pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, farMedMellomnavn, skriftsspraak);

    // then
    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Termindato"),
        () -> assertThat(dokumenttekst).contains("Opplysningar om barnet"),
        () ->
            assertThat(dokumenttekst)
                .contains("Fødselsnummer: " + NYFOEDT_BARN.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Fødselsdato: "
                        + FOEDSELSDATO_NYFOEDT_BARN.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Fødestad: " + nyfoedtBarn.getFoedested()));

    validereInformasjonOmForeldrenePaaNorsk(dokumenttekst, mor, farMedMellomnavn, skriftsspraak);

    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }
  }

  @Test
  void skalGenererePdfPaaEngelskForForelderMedMellomnavn() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer()))
        .thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var nyfoedtBarn = mapper.toDto(NYFOEDT_BARN);
    nyfoedtBarn.setFoedested("Ås");
    nyfoedtBarn.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);
    var mor = mapper.toDto(MOR);
    var farMedMellomnavn = mapper.toDto(henteForelder(Forelderrolle.FAR));
    var farsOpprinneligeNavn = NAVN_FAR;

    farMedMellomnavn.setNavn(
        NavnDto.builder()
            .fornavn(farsOpprinneligeNavn.getFornavn())
            .mellomnavn("Strømstad")
            .etternavn(farsOpprinneligeNavn.getEtternavn())
            .build());

    // when
    var pdfstroem =
        pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, farMedMellomnavn, Skriftspraak.ENGELSK);

    // then
    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Expected date of birth "),
        () -> assertThat(dokumenttekst).contains("Child"),
        () ->
            assertThat(dokumenttekst)
                .contains("Social security number: " + NYFOEDT_BARN.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Date of birth: "
                        + FOEDSELSDATO_NYFOEDT_BARN.format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Place of birth: " + nyfoedtBarn.getFoedested()));

    validereInformasjonOmForeldrenePaaEngelsk(dokumenttekst, mor, farMedMellomnavn);
  }

  private void skriveUtPdfForInspeksjon(byte[] pdfstroem) {
    try (final FileOutputStream filstroem = new FileOutputStream("farskapserklaering.pdf")) {
      filstroem.write(pdfstroem);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private void validereInformasjonOmForeldrenePaaNorsk(
      String dokumenttekst, ForelderDto mor, ForelderDto far, Skriftspraak norskSkriftsspraak) {
    var navnFar = far.getNavn().sammensattNavn();
    var navnMor = mor.getNavn().sammensattNavn();

    var opplysninger =
        norskSkriftsspraak.equals(Skriftspraak.NYNORSK) ? "Opplysningar" : "Opplysninger";
    var navn = norskSkriftsspraak.equals(Skriftspraak.NYNORSK) ? "Namn" : "Navn";

    assertAll(
        () -> assertThat(dokumenttekst).contains(opplysninger + " om mor"),
        () -> assertThat(dokumenttekst).contains("Fødselsnummer: " + mor.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Fødselsdato: "
                        + mor.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains(navn + ": " + navnMor),
        () -> assertThat(dokumenttekst).contains(opplysninger + " om far"),
        () -> assertThat(dokumenttekst).contains("Fødselsnummer: " + far.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Fødselsdato: "
                        + far.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains(navn + ": " + navnFar));
  }

  private void validereInformasjonOmForeldrenePaaEngelsk(
      String dokumenttekst, ForelderDto mor, ForelderDto far) {

    var navnFar = far.getNavn().sammensattNavn();

    var navnMor = mor.getNavn().sammensattNavn();

    assertAll(
        () -> assertThat(dokumenttekst).contains("Mother"),
        () ->
            assertThat(dokumenttekst)
                .contains("Social security number: " + mor.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Date of birth: "
                        + mor.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Name: " + navnMor),
        () -> assertThat(dokumenttekst).contains("Father"),
        () ->
            assertThat(dokumenttekst)
                .contains("Social security number: " + far.getFoedselsnummer()),
        () ->
            assertThat(dokumenttekst)
                .contains(
                    "Date of birth: "
                        + far.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Name: " + navnFar));
  }
}
