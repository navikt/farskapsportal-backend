package no.nav.farskapsportal.consumer.pdf;

import static no.nav.farskapsportal.api.Feilkode.OPPRETTE_PDF_FEILET;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.api.Skriftspraak;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.PDFConsumerException;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
@Slf4j
public class PdfGeneratorConsumer {

  private static final String STI_TIL_PDF_TEMPLATE = "/pdf-template/";
  private static final Map<Elementnavn, String> elementnavnTilEngelsk = Map.ofEntries(
      new AbstractMap.SimpleEntry<>(Elementnavn.BARN, "child"),
      new AbstractMap.SimpleEntry<>(Elementnavn.BESKRIVELSE, "description"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FOEDSELSDATO, "date-of-birth"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FOEDSELSNUMMER, "ssn"),
      new AbstractMap.SimpleEntry<>(Elementnavn.MOR, "mother"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FAR, "father"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FOEDESTED, "place-of-birth"),
      new AbstractMap.SimpleEntry<>(Elementnavn.NAVN, "name"),
      new AbstractMap.SimpleEntry<>(Elementnavn.NYFOEDT, "newborn"),
      new AbstractMap.SimpleEntry<>(Elementnavn.TERMINDATO, "due-date"),
      new AbstractMap.SimpleEntry<>(Elementnavn.UFOEDT, "unborn")
  );
  private static final Map<Tekst, String> tekstBokmaal = Map.of(
      Tekst.FOEDSELSDATO, "Fødselsdato",
      Tekst.FOEDSELSNUMMER, "Fødselsnummer",
      Tekst.FOEDESTED, "Fødested",
      Tekst.NAVN, "Navn",
      Tekst.OPPLYSNINGER_OM_BARNET, "Opplysninger om barnet",
      Tekst.TERMINDATO, "Termindato"
  );
  private static final Map<Tekst, String> tekstEngelsk = Map.of(
      Tekst.FOEDSELSDATO, "Date of birth",
      Tekst.FOEDSELSNUMMER, "Social security number",
      Tekst.FOEDESTED, "Place of birth",
      Tekst.NAVN, "Name",
      Tekst.OPPLYSNINGER_OM_BARNET, "Child",
      Tekst.TERMINDATO, "Expected date of birth"
  );

  public byte[] genererePdf(BarnDto barnMedDetaljer, ForelderDto morMedDetaljer, ForelderDto farMedDetaljer, Skriftspraak skriftspraak) {

    log.info("Oppretter dokument for farskapserklæring på {}", skriftspraak);

    var html = byggeHtmlstrengFraMal(STI_TIL_PDF_TEMPLATE, skriftspraak, barnMedDetaljer, morMedDetaljer,
        farMedDetaljer);
    try (final ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {

      var htmlSomStroem = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
      org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlSomStroem, "UTF-8", "pdf-template/template.html");
      Document doc = new W3CDom().fromJsoup(jsoupDoc);
      var builder = new PdfRendererBuilder();

      try (InputStream colorProfile = PdfGeneratorConsumer.class.getResourceAsStream("/pdf-template/ISOcoated_v2_300_bas.ICC")) {
        byte[] colorProfileBytes = IOUtils.toByteArray(colorProfile);
        builder.useColorProfile(colorProfileBytes);
      }

      try (InputStream fontStream = PdfGeneratorConsumer.class.getResourceAsStream("/pdf-template/Arial.ttf")) {
        byte[] fontBytes = IOUtils.toByteArray(fontStream);
        FileUtils.writeByteArrayToFile(new File("Arial.ttf"), fontBytes);
        builder.useFont(new File("Arial.ttf"), "ArialNormal");
      }

      try {
        builder.useProtocolsStreamImplementation(new ClassPathStreamFactory(), "classpath")
            .useFastMode()
            .usePdfAConformance(PdfAConformance.PDFA_2_A)
            .withW3cDocument(doc, "classpath:/pdf-template/")
            .toStream(pdfStream)
            .run();

      } catch (Exception e) {
        e.printStackTrace();
      }

      var innhold = pdfStream.toByteArray();
      pdfStream.close();

      return innhold;
    } catch (IOException ioe) {
      throw new PDFConsumerException(OPPRETTE_PDF_FEILET, ioe);
    }
  }

  private void leggeTilDataBarn(org.jsoup.nodes.Element barnElement, BarnDto barnDto, Skriftspraak skriftspraak) {
    if (barnDto.getFoedselsnummer() != null) {
      barnElement.getElementsByClass(henteElementnavn(Elementnavn.UFOEDT, skriftspraak)).remove();
      var beskrivelse = barnElement.getElementsByClass(henteElementnavn(Elementnavn.BESKRIVELSE, skriftspraak));
      beskrivelse.first().text(tekstvelger(Tekst.OPPLYSNINGER_OM_BARNET, skriftspraak));
      var foedselsdato = barnElement.getElementsByClass(henteElementnavn(Elementnavn.FOEDSELSDATO, skriftspraak));
      foedselsdato.first()
          .text(tekstvelger(Tekst.FOEDSELSDATO, skriftspraak) + ": " + barnDto.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
      var foedselsnummer = barnElement.getElementById(henteElementnavn(Elementnavn.FOEDSELSNUMMER, skriftspraak));
      foedselsnummer.text(tekstvelger(Tekst.FOEDSELSNUMMER, skriftspraak) + ": " + barnDto.getFoedselsnummer());
      if (barnDto.getFoedested() != null) {
        var foedested = barnElement.getElementById(henteElementnavn(Elementnavn.FOEDESTED, skriftspraak));
        foedested.text(tekstvelger(Tekst.FOEDESTED, skriftspraak) + ": " + barnDto.getFoedested());
      } else {
        barnElement.getElementById(henteElementnavn(Elementnavn.FOEDESTED, skriftspraak)).remove();
      }
    } else {
      barnElement.getElementsByClass(henteElementnavn(Elementnavn.NYFOEDT, skriftspraak)).remove();
      var termindato = barnElement.getElementById(henteElementnavn(Elementnavn.TERMINDATO, skriftspraak));
      termindato.text(tekstvelger(Tekst.TERMINDATO, skriftspraak) + ": " + barnDto.getTermindato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }
  }

  private void leggeTilDataForelder(org.jsoup.nodes.Element forelderelement, ForelderDto forelderDto, Skriftspraak skriftspraak) {
    var navn = forelderelement.getElementsByClass(henteElementnavn(Elementnavn.NAVN, skriftspraak));

    navn.first().text(tekstvelger(Tekst.NAVN, skriftspraak) + ": " + forelderDto.getNavn().sammensattNavn());

    var foedselsdato = forelderelement.getElementsByClass(henteElementnavn(Elementnavn.FOEDSELSDATO, skriftspraak));
    foedselsdato.first().text(
        tekstvelger(Tekst.FOEDSELSDATO, skriftspraak) + ": " + forelderDto.getFoedselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

    var foedselsnummer = forelderelement.getElementsByClass(henteElementnavn(Elementnavn.FOEDSELSNUMMER, skriftspraak));
    foedselsnummer.first().text(tekstvelger(Tekst.FOEDSELSNUMMER, skriftspraak) + ": " + forelderDto.getFoedselsnummer());
  }

  private String byggeHtmlstrengFraMal(String pdfmal, Skriftspraak skriftspraak, BarnDto barn, ForelderDto mor, ForelderDto far) {
    try {
      var input = new ClassPathResource(pdfmal + skriftspraak.toString().toLowerCase() + ".html").getInputStream();
      var document = Jsoup.parse(input, "UTF-8", "");

      // Legge til informasjon om barn
      leggeTilDataBarn(document.getElementById(henteElementnavn(Elementnavn.BARN, skriftspraak)), barn, skriftspraak);
      // Legge til informasjon om mor
      leggeTilDataForelder(document.getElementById(henteElementnavn(Elementnavn.MOR, skriftspraak)), mor, skriftspraak);
      // Legge til informasjon om far
      leggeTilDataForelder(document.getElementById(henteElementnavn(Elementnavn.FAR, skriftspraak)), far, skriftspraak);

      // jsoup fjerner tagslutt for <link> og <meta> - legger på manuelt ettersom dette er påkrevd av PDFBOX
      var html = document.html().replaceFirst("charset=utf-8\">", "charset=utf-8\"/>");
      html = html.replaceFirst("href=\"style.css\">", "href=\"style.css\"/>");

      return html;

    } catch (IOException ioe) {
      throw new PDFConsumerException(OPPRETTE_PDF_FEILET, ioe);
    }
  }

  private String henteElementnavn(Elementnavn element, Skriftspraak skriftspraak) {

    switch (skriftspraak) {
      case ENGELSK -> {
        return elementnavnTilEngelsk.get(element);
      }
    }

    // bokmål
    return element.toString().toLowerCase();
  }

  private String tekstvelger(Tekst tekst, Skriftspraak skriftspraak) {
    switch (skriftspraak) {
      case ENGELSK -> {
        return tekstEngelsk.get(tekst);
      }
      default -> {
        return tekstBokmaal.get(tekst);
      }
    }
  }

  private enum Tekst {
    FOEDSELSDATO,
    FOEDSELSNUMMER,
    FOEDESTED,
    NAVN,
    OPPLYSNINGER_OM_BARNET,
    TERMINDATO;
  }

  private enum Elementnavn {
    BARN,
    BESKRIVELSE,
    FAR,
    FOEDSELSDATO,
    FOEDSELSNUMMER,
    FOEDESTED,
    MOR,
    NAVN,
    NYFOEDT,
    TERMINDATO,
    UFOEDT;
  }
}
