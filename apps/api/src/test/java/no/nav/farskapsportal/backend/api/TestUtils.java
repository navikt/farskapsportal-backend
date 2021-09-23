package no.nav.farskapsportal.backend.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.backend.lib.dto.BarnDto;
import no.nav.farskapsportal.backend.lib.dto.DokumentDto;
import no.nav.farskapsportal.backend.lib.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.backend.lib.dto.ForelderDto;
import no.nav.farskapsportal.backend.lib.dto.Forelderrolle;
import no.nav.farskapsportal.backend.lib.dto.NavnDto;

public class TestUtils {

  public final static LocalDate FOEDSELSDATO_FAR = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
  public final static LocalDate FOEDSELSDATO_MOR = FOEDSELSDATO_FAR.plusYears(4);
  public final static ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  public final static ForelderDto MOR = henteForelder(Forelderrolle.MOR);

  public static URI lageUrl(String kontekst) {
    try {
      return new URI("http://localhost:8096" + kontekst);
    } catch (URISyntaxException uriSyntaxException) {
      throw new RuntimeException("Feil syntaks i test URI");
    }
  }

  public static URI tilUri(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException uriSyntaxException) {
      throw new RuntimeException("Feil syntaks i test URI");
    }
  }

  public static FarskapserklaeringDto henteFarskapserklaeringDto(ForelderDto mor, ForelderDto far, BarnDto barn) {

    var dokument = DokumentDto.builder().dokumentnavn("farskapserklaering.pdf").redirectUrlMor(lageUrl("redirect-mor"))
        .redirectUrlFar(lageUrl("/redirect-far"))
        .build();

    return FarskapserklaeringDto.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }

  public static BarnDto henteBarnUtenFnr(int antallUkerTilTermindato) {
    var termindato = LocalDate.now().plusDays(7 * antallUkerTilTermindato);
    return BarnDto.builder().termindato(termindato).build();
  }

  public static BarnDto henteNyligFoedtBarn() {
    var personnummer = "12340";
    var foedselsdato = LocalDate.now().minusMonths(2).minusDays(13);
    var fnrBarn = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
    return BarnDto.builder().foedselsnummer(fnrBarn).foedselsdato(foedselsdato).foedested("Stange").build();
  }

  public static BarnDto henteBarnMedFnr(LocalDate foedselsdato) {
    var personnummer = "12340";
    return henteBarnMedFnr(foedselsdato, personnummer);
  }

  public static BarnDto henteBarnMedFnr(LocalDate foedselsdato, String personnummer) {
    var fnrBarn = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
    return BarnDto.builder().foedselsnummer(fnrBarn).foedselsdato(foedselsdato).foedested("HAMAR").build();
  }

  public static ForelderDto henteForelder(Forelderrolle forelderrolle) {
    if (Forelderrolle.MOR.equals(forelderrolle)) {
      var personnummerMor = "12340";
      return ForelderDto.builder()
          .foedselsnummer(FOEDSELSDATO_MOR.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerMor)
          .foedselsdato(FOEDSELSDATO_MOR)
          .navn(NavnDto.builder().fornavn("Ronaldina").etternavn("McDonald").build()).build();
    } else {
      var personnummerFar = "12345";
      return ForelderDto.builder()
          .foedselsnummer(FOEDSELSDATO_FAR.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar)
          .foedselsdato(FOEDSELSDATO_FAR)
          .navn(NavnDto.builder().fornavn("Ronald").etternavn("McDonald").build()).build();
    }
  }
}
