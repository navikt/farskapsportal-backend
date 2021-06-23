package no.nav.farskapsportal.consumer.pdf;

import static no.nav.farskapsportal.api.Feilkode.OPPRETTE_PDF_FEILET;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.PDFConsumerException;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
@Slf4j
public class PdfGeneratorConsumer {

  public byte[] genererePdf(BarnDto barnMedDetaljer, ForelderDto morMedDetaljer, ForelderDto farMedDetaljer)  {
    log.info("Oppretter dokument for farskapserklæring");

    var html = byggeHtmlstrengFraMal("/pdf-template/template.html", barnMedDetaljer, morMedDetaljer, farMedDetaljer);
    try (final ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {

      var htmlSomStroem = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
      org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlSomStroem, "UTF-8", "pdf-template/template.html");
      Document doc = new W3CDom().fromJsoup(jsoupDoc);

      new PdfRendererBuilder()
          .useProtocolsStreamImplementation(new ClassPathStreamFactory(), "classpath")
          .useFastMode()
          .withW3cDocument(doc, "classpath:/pdf-template/")
          .toStream(pdfStream)
          .run();

      return pdfStream.toByteArray();
    } catch(IOException ioe) {
      throw new PDFConsumerException(OPPRETTE_PDF_FEILET, ioe);
    }
  }


  private void leggeTilDataBarn(Element barnElement, BarnDto barnDto) {
    if (barnDto.getFoedselsnummer() != null) {
      barnElement.getElementsByClass("ufoedt").remove();
      var beskrivelse = barnElement.getElementsByClass("beskrivelse");
      beskrivelse.first().text("Opplysninger om barnet");
      var foedselsdato = barnElement.getElementsByClass("foedselsdato");
      foedselsdato.first().text("Fødselsdato: " + barnDto.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
      var foedselsnummer = barnElement.getElementById("foedselsnummer");
      foedselsnummer.text("Fødselsnummer: " + barnDto.getFoedselsnummer());
      if (barnDto.getFoedested() != null) {
        var foedested = barnElement.getElementById("foedested");
        foedested.text("Fødested: " + barnDto.getFoedested());
      } else {
        barnElement.getElementById("foedested").remove();
      }
    } else {
      barnElement.getElementsByClass("nyfoedt").remove();
      var termindato = barnElement.getElementById("termindato");
      termindato.text("Termindato: " + barnDto.getTermindato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }
  }

  private void leggeTilDataForelder(Element forelderelement, ForelderDto forelderDto) {
    var navn = forelderelement.getElementsByClass("navn");
    navn.first().text("Navn: " + forelderDto.getFornavn()
        + henteMellomnavnHvisRegistrert(forelderDto)
        + forelderDto.getEtternavn());

    var foedselsdato = forelderelement.getElementsByClass("foedselsdato");
    foedselsdato.first().text("Fødselsdato: " + forelderDto.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

    var foedselsnummer = forelderelement.getElementsByClass("foedselsnummer");
    foedselsnummer.first().text("Fødselsnummer: " + forelderDto.getFoedselsnummer());
  }

  private String byggeHtmlstrengFraMal(String stiHtmlMal, BarnDto barn, ForelderDto mor, ForelderDto far) {
    try {
      var input = new ClassPathResource(stiHtmlMal).getInputStream();
      var document = Jsoup.parse(input, "UTF-8", "");

      // Legge til informasjon om barn
      leggeTilDataBarn(document.getElementById("barn"), barn);
      // Legge til informasjon om mor
      leggeTilDataForelder(document.getElementById("mor"), mor);
      // Legge til informasjon om far
      leggeTilDataForelder(document.getElementById("far"), far);

      // jsoup fjerner tagslutt for <link> og <meta> - legger på manuelt ettersom dette er påkrevd av PDFBOX
      var html = document.html().replaceFirst("charset=utf-8\">", "charset=utf-8\"/>");
      html = html.replaceFirst("href=\"style.css\">", "href=\"style.css\"/>");

      return html;

    } catch (IOException ioe) {
      throw new PDFConsumerException(OPPRETTE_PDF_FEILET, ioe);
    }
  }

  private String henteMellomnavnHvisRegistrert(ForelderDto forelderDto) {
    return forelderDto.getMellomnavn() != null ? " " + forelderDto.getMellomnavn() + " " : " ";
  }
}
