package no.nav.farskapsportal.consumer.pdf;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PdfGeneratorConsumer {

  public DokumentDto genererePdf(FarskapserklaeringDto dto) {
    log.info("Oppretter dokument for farskapserklæring");

    var dokumentnavn = "Farskapsportal.pdf";

    try (var pdf = new PDDocument()) {
    var side = new PDPage();
    pdf.addPage(side);
      var innhold = new PDPageContentStream(pdf, side);
      innhold.setFont(PDType1Font.COURIER, 12);
      innhold.beginText();
      innhold.showText("Farskapserklæring");
      innhold.showText("Barn");
      if (dto.getBarn().getFoedselsnummer() != null
          && dto.getBarn().getFoedselsnummer().length() > 0) {
        innhold.showText("Fødselsnummer: " + dto.getBarn().getFoedselsnummer());
      } else {
        innhold.showText("Termindato: " + dto.getBarn().getTermindato().toString());
      }

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("Mor");
      innhold.showText("Fødselsnummer: " + dto.getMor().getFoedselsnummer());
      innhold.showText("Navn: " + slaaSammenNavn(dto.getMor()));
      innhold.showText("");

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("Mor:");
      innhold.showText("Fødselsnummer: " + dto.getMor().getFoedselsnummer());
      innhold.showText("Navn: " + slaaSammenNavn(dto.getMor()));
      innhold.showText("");

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("Far");
      innhold.showText("Fødselsnummer: " + dto.getFar().getFoedselsnummer());
      innhold.showText("Navn: " + slaaSammenNavn(dto.getFar()));
      innhold.showText("");
      innhold.endText();
      innhold.close();
      pdf.save(dokumentnavn);

      PDStream pdStream = new PDStream(pdf);

      return DokumentDto.builder()
          .dokumentnavn(dokumentnavn)
          .innhold(pdStream.toByteArray())
          .build();

    } catch (IOException ioe) {
      throw new PdfgenereringFeiletException(
          "Opprettelse av PDF-dokument for farskapserklæring feilet!");
    }
  }

  private String slaaSammenNavn(ForelderDto forelder) {
    return forelder.getFornavn()
        + " "
        + forelder.getMellomnavn()
        + (forelder.getMellomnavn() != null && forelder.getMellomnavn().length() > 0 ? " " : "")
        + forelder.getEtternavn();
  }
}
