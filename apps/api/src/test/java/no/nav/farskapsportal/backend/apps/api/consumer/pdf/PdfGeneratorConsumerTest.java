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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.api.Skriftspraak;
import no.nav.farskapsportal.backend.libs.dto.ForelderDto;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.libs.felles.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.felles.util.Mapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PdfGeneratorConsumerTest")
@SpringBootTest(classes =  FarskapsportalApiApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class PdfGeneratorConsumerTest {

  private static final Forelder MOR = henteForelder(Forelderrolle.MOR);
  private static final Forelder FAR = henteForelder(Forelderrolle.FAR);
  private static final Barn UFOEDT_BARN = henteBarnUtenFnr(17);
  private static final Barn NYFOEDT_BARN = henteNyligFoedtBarn();
  private static boolean skriveUtPdf = false;

  @Autowired
  private PdfGeneratorConsumer pdfGeneratorConsumer;

  @Autowired
  private Mapper mapper;

  @MockBean
  private PersonopplysningService personopplysningServiceMock;

  @Test
  void skalGenererePdfPaaBokmaalForUfoedt() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var ufoedtBarn = mapper.toDto(UFOEDT_BARN);
    var mor = mapper.toDto(MOR);
    mor.setForelderrolle(Forelderrolle.MOR);

    var far = mapper.toDto(FAR);
    far.setForelderrolle(Forelderrolle.FAR);

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(ufoedtBarn, mor, far, Skriftspraak.BOKMAAL);

    // then
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstroem);
    }

    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Fødested"),
        () -> assertThat(dokumenttekst).contains("Termindato: " + UFOEDT_BARN.getTermindato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
    );

    validereInformasjonOmForeldrenePaaBokmaal(dokumenttekst, mor, far);

  }

  @Test
  void skalGenererePdfPaaEngelskForUfoedt() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
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
        () -> assertThat(dokumenttekst).contains(
            "Expected date of birth: " + UFOEDT_BARN.getTermindato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
    );

    validereInformasjonOmForeldrenePaaEngelsk(dokumenttekst, mor, far);
  }

  @Test
  void skalGenererePdfPaaBokmaalForNyfoedt() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
    when(personopplysningServiceMock.henteNavn(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);

    var nyfoedtBarn = mapper.toDto(NYFOEDT_BARN);
    nyfoedtBarn.setFoedested("Stange");
    nyfoedtBarn.setFoedselsdato(FOEDSELSDATO_NYFOEDT_BARN);

    var mor = mapper.toDto(MOR);
    mor.setForelderrolle(Forelderrolle.MOR);

    var far = mapper.toDto(FAR);
    far.setForelderrolle(Forelderrolle.FAR);

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, far, Skriftspraak.BOKMAAL);

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
        () -> assertThat(dokumenttekst).contains("Fødselsnummer: " + NYFOEDT_BARN.getFoedselsnummer()),
        () -> assertThat(dokumenttekst).contains("Fødselsdato: " + FOEDSELSDATO_NYFOEDT_BARN.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Fødested: " + nyfoedtBarn.getFoedested())
    );

    validereInformasjonOmForeldrenePaaBokmaal(dokumenttekst, mor, far);
  }

  @Test
  void skalGenererePdfPaaEngelskForNyfoedt() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
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
        () -> assertThat(dokumenttekst).contains("Social security number: " + NYFOEDT_BARN.getFoedselsnummer()),
        () -> assertThat(dokumenttekst).contains(
            "Date of birth: " + FOEDSELSDATO_NYFOEDT_BARN.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Place of birth: " + nyfoedtBarn.getFoedested())
    );

    validereInformasjonOmForeldrenePaaEngelsk(dokumenttekst, mor, far);
  }

  @Test
  void skalGenererePdfPaaBokmaalForForelderMedMellomnavn() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
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
            .etternavn(farsOpprinneligeNavn.getEtternavn()).build());

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, farMedMellomnavn, Skriftspraak.BOKMAAL);

    // then
    PDDocument doc = PDDocument.load(pdfstroem);
    PDFTextStripper pdfTextStripper = new PDFTextStripper();
    String dokumenttekst = pdfTextStripper.getText(doc);

    assertAll(
        () -> assertThat(dokumenttekst).doesNotContain("Termindato"),
        () -> assertThat(dokumenttekst).contains("Opplysninger om barnet"),
        () -> assertThat(dokumenttekst).contains("Fødselsnummer: " + NYFOEDT_BARN.getFoedselsnummer()),
        () -> assertThat(dokumenttekst).contains("Fødselsdato: " + FOEDSELSDATO_NYFOEDT_BARN.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Fødested: " + nyfoedtBarn.getFoedested())
    );

    validereInformasjonOmForeldrenePaaBokmaal(dokumenttekst, mor, farMedMellomnavn);
  }

  @Test
  void skalGenererePdfPaaEngelskForForelderMedMellomnavn() throws IOException {

    // given
    when(personopplysningServiceMock.henteFoedselsdato(MOR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningServiceMock.henteNavn(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningServiceMock.henteFoedselsdato(FAR.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);
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
            .etternavn(farsOpprinneligeNavn.getEtternavn()).build());

    // when
    var pdfstroem = pdfGeneratorConsumer.genererePdf(nyfoedtBarn, mor, farMedMellomnavn, Skriftspraak.ENGELSK);

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
        () -> assertThat(dokumenttekst).contains("Social security number: " + NYFOEDT_BARN.getFoedselsnummer()),
        () -> assertThat(dokumenttekst).contains(
            "Date of birth: " + FOEDSELSDATO_NYFOEDT_BARN.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Place of birth: " + nyfoedtBarn.getFoedested())
    );

    validereInformasjonOmForeldrenePaaEngelsk(dokumenttekst, mor, farMedMellomnavn);
  }


  private void skriveUtPdfForInspeksjon(byte[] pdfstroem) {
    try (final FileOutputStream filstroem = new FileOutputStream("farskapserklaering.pdf")) {
      filstroem.write(pdfstroem);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }


  private void validereInformasjonOmForeldrenePaaBokmaal(String dokumenttekst, ForelderDto mor, ForelderDto far) {

    var navnFar = far.getNavn().sammensattNavn();

    var navnMor = mor.getNavn().sammensattNavn();

    assertAll(
        () -> assertThat(dokumenttekst).contains("Opplysninger om mor"),
        () -> assertThat(dokumenttekst).contains("Fødselsnummer: " + mor.getFoedselsnummer()),
        () -> assertThat(dokumenttekst).contains("Fødselsdato: " + mor.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Navn: " + navnMor),
        () -> assertThat(dokumenttekst).contains("Opplysninger om far"),
        () -> assertThat(dokumenttekst).contains("Fødselsnummer: " + far.getFoedselsnummer()),
        () -> assertThat(dokumenttekst).contains("Fødselsdato: " + far.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Navn: " + navnFar)
    );
  }

  private void validereInformasjonOmForeldrenePaaEngelsk(String dokumenttekst, ForelderDto mor, ForelderDto far) {

    var navnFar = far.getNavn().sammensattNavn();

    var navnMor = mor.getNavn().sammensattNavn();

    assertAll(
        () -> assertThat(dokumenttekst).contains("Mother"),
        () -> assertThat(dokumenttekst).contains("Social security number: " + mor.getFoedselsnummer()),
        () -> assertThat(dokumenttekst).contains("Date of birth: " + mor.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Name: " + navnMor),
        () -> assertThat(dokumenttekst).contains("Father"),
        () -> assertThat(dokumenttekst).contains("Social security number: " + far.getFoedselsnummer()),
        () -> assertThat(dokumenttekst).contains("Date of birth: " + far.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
        () -> assertThat(dokumenttekst).contains("Name: " + navnFar)
    );
  }
}
