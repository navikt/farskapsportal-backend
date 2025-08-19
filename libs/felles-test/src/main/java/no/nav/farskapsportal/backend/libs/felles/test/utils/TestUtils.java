package no.nav.farskapsportal.backend.libs.felles.test.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.KodeLand;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Forelder;

public class TestUtils {

  public static final LocalDate FOEDSELSDATO_FAR =
      LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
  public static final LocalDate FOEDSELSDATO_MOR = FOEDSELSDATO_FAR.plusYears(4);
  public static final String FØDELAND_MOR = KodeLand.NORGE.getKodeLand();
  public static final String FØDELAND_FAR = KodeLand.NORGE.getKodeLand();
  public static final Forelder FAR = henteForelder(Forelderrolle.FAR);
  public static final Forelder MOR = henteForelder(Forelderrolle.MOR);
  public static final LocalDate FOEDSELSDATO_NYFOEDT_BARN =
      LocalDate.now().minusMonths(2).minusDays(13);
  public static final NavnDto NAVN_FAR =
      NavnDto.builder().fornavn("Ronald").etternavn("McDonald").build();
  public static final NavnDto NAVN_MOR =
      NavnDto.builder().fornavn("Ronaldina").etternavn("McDonald").build();

  public static String lageUrl(String port, String kontekst) {
    return "http://localhost:" + port + kontekst;
  }

  public static URI lageUri(String port, String kontekst) {
    try {
      return new URI(lageUrl(port, kontekst));
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

  public static Barn henteBarnUtenFnr(int antallUkerTilTermindato) {
    var termindato = LocalDate.now().plusDays(7 * antallUkerTilTermindato);
    return Barn.builder().termindato(termindato).build();
  }

  public static Barn henteNyligFoedtBarn() {
    var personnummer = "12340";
    var foedselsdato = FOEDSELSDATO_NYFOEDT_BARN;
    var fnrBarn = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
    return Barn.builder().foedselsnummer(fnrBarn).build();
  }

  public static Barn henteBarnMedFnr(LocalDate foedselsdato) {
    var personnummer = "12340";
    return henteBarnMedFnr(foedselsdato, personnummer);
  }

  public static Barn henteBarnMedFnr(LocalDate foedselsdato, String personnummer) {
    var fnrBarn = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
    return Barn.builder().foedselsnummer(fnrBarn).build();
  }

  public static Forelder henteForelder(Forelderrolle forelderrolle) {
    if (Forelderrolle.MOR.equals(forelderrolle)) {
      var personnummerMor = "12340";
      return Forelder.builder()
          .foedselsnummer(
              FOEDSELSDATO_MOR.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerMor)
          .build();
    } else {
      var personnummerFar = "12345";
      return Forelder.builder()
          .foedselsnummer(
              FOEDSELSDATO_FAR.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar)
          .build();
    }
  }
}
