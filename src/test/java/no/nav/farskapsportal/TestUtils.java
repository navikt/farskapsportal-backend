package no.nav.farskapsportal;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;

public class TestUtils {

  public static URI lageUrl(String kontekst) {
    try {
      return new URI("https://esignering.no/" + kontekst);
    } catch (URISyntaxException uriSyntaxException) {
      throw new RuntimeException("Feil syntaks i test URI");
    }
  }

  public static FarskapserklaeringDto henteFarskapserklaering(
      ForelderDto mor, ForelderDto far, BarnDto barn) {

    var dokument =
        DokumentDto.builder()
            .dokumentnavn("farskapserklaering.pdf")
            .padesUrl(lageUrl("pades"))
            .redirectUrlMor(lageUrl("redirect-mor"))
            .redirectUrlFar(lageUrl("redirect-far"))
            .build();

    return FarskapserklaeringDto.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }

  public static BarnDto henteBarn(int antallMndTilTermindato) {
    var termindato = LocalDate.now().plusMonths(antallMndTilTermindato);
    return BarnDto.builder().termindato(termindato).build();
  }

  public static ForelderDto henteForelder(Forelderrolle forelderrolle) {
    if (Forelderrolle.MOR.equals(forelderrolle)) {
      var personnummerMor = "12340";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);

      return ForelderDto.builder()
          .foedselsnummer(
              foedselsdato.plusYears(4).format(DateTimeFormatter.ofPattern("ddMMyy"))
                  + personnummerMor)
          .fornavn("Ronaldina")
          .etternavn("McDonald")
          .forelderrolle(Forelderrolle.MOR)
          .build();
    } else {
      var personnummerFar = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);

      return ForelderDto.builder()
          .foedselsnummer(
              foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar)
          .fornavn("Ronald")
          .etternavn("McDonald")
          .forelderrolle(Forelderrolle.FAR)
          .build();
    }
  }

}
