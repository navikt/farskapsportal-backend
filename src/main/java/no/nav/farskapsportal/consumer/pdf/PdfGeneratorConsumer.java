package no.nav.farskapsportal.consumer.pdf;

import java.io.IOException;
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
public class PdfGeneratorConsumer {

  public DokumentDto genererePdf(FarskapserklaeringDto dto) {

    var dokumentnavn = "Farskapsportal.pdf";

    var pdf = new PDDocument();
    var side = new PDPage();
    pdf.addPage(side);
    try {
      var innhold = new PDPageContentStream(pdf, side);
      innhold.setFont(PDType1Font.COURIER, 12);
      innhold.beginText();
      innhold.showText("Farskapserklæring\n");

      innhold.showText("\nBarn");
      if (dto.getBarn().getFoedselsnummer() != null
          && dto.getBarn().getFoedselsnummer().length() > 0) {
        innhold.showText("\nFødselsnummer: " + dto.getBarn().getFoedselsnummer());
      } else {
        innhold.showText("\nTermindato: " + dto.getBarn().getTermindato().toString());
      }

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("\nMor");
      innhold.showText("\nFødselsnummer: " + dto.getMor().getFoedselsnummer());
      innhold.showText("\nNavn: " + slaaSammenNavn(dto.getMor()));
      innhold.showText("\n");

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("\nMor:");
      innhold.showText("\nFødselsnummer: " + dto.getMor().getFoedselsnummer());
      innhold.showText("\nNavn: " + slaaSammenNavn(dto.getMor()));
      innhold.showText("\n");

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("\nFar");
      innhold.showText("\nFødselsnummer: " + dto.getFar().getFoedselsnummer());
      innhold.showText("Navn: " + slaaSammenNavn(dto.getFar()));
      innhold.showText("\n");

      innhold.endText();
      innhold.close();
      pdf.save(dokumentnavn);
      pdf.close();

      PDStream pdStream = new PDStream(pdf);

      return DokumentDto.builder()
          .dokumentnavn(dokumentnavn)
          .dokument(pdStream.toByteArray())
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
        + (forelder.getMellomnavn().length() > 0 ? " " : "")
        + forelder.getEtternavn();
  }
}
