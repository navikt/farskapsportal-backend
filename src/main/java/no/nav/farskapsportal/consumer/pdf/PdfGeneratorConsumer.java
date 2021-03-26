package no.nav.farskapsportal.consumer.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PdfGeneratorConsumer {

  public Dokument genererePdf(Farskapserklaering farskapserklaering) {
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
      if (farskapserklaering.getBarn().getFoedselsnummer() != null && farskapserklaering.getBarn().getFoedselsnummer().length() > 0) {
        innhold.showText("Fødselsnummer: " + farskapserklaering.getBarn().getFoedselsnummer());
      } else {
        innhold.showText("Termindato: " + farskapserklaering.getBarn().getTermindato().toString());
      }

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("Mor");
      innhold.showText("Fødselsnummer: " + farskapserklaering.getMor().getFoedselsnummer());
      innhold.showText("Navn: " + slaaSammenNavn(farskapserklaering.getMor()));
      innhold.showText("");

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("Mor:");
      innhold.showText("Fødselsnummer: " + farskapserklaering.getMor().getFoedselsnummer());
      innhold.showText("Navn: " + slaaSammenNavn(farskapserklaering.getMor()));
      innhold.showText("");

      for (int i = 0; i < 25; i++) {
        innhold.showText("-");
      }

      innhold.showText("Far");
      innhold.showText("Fødselsnummer: " + farskapserklaering.getFar().getFoedselsnummer());
      innhold.showText("Navn: " + slaaSammenNavn(farskapserklaering.getFar()));
      innhold.showText("");
      innhold.endText();
      innhold.close();
      pdf.save(dokumentnavn);
      var baos = new ByteArrayOutputStream();
      pdf.save(baos);
      pdf.close();

      return Dokument.builder().dokumentnavn(dokumentnavn).dokumentinnhold(Dokumentinnhold.builder().innhold(baos.toByteArray()).build()).build();

    } catch (IOException ioe) {
      throw new PdfgenereringFeiletException("Opprettelse av PDF-dokument for farskapserklæring feilet!");
    }
  }

  private String slaaSammenNavn(Forelder forelder) {
    return forelder.getFornavn() + " " + forelder.getMellomnavn() + (forelder.getMellomnavn() != null && forelder.getMellomnavn().length() > 0 ? " "
        : "") + forelder.getEtternavn();
  }
}
