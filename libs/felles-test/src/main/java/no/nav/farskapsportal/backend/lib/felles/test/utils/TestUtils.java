package no.nav.farskapsportal.backend.lib.felles.test.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.backend.lib.dto.Forelderrolle;
import no.nav.farskapsportal.backend.lib.dto.NavnDto;
import no.nav.farskapsportal.backend.lib.entity.Barn;
import no.nav.farskapsportal.backend.lib.entity.Dokument;
import no.nav.farskapsportal.backend.lib.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.lib.entity.Forelder;
import no.nav.farskapsportal.backend.lib.entity.Signeringsinformasjon;

public class TestUtils {

  public static final LocalDate FOEDSELSDATO_FAR = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
  public static final LocalDate FOEDSELSDATO_MOR = FOEDSELSDATO_FAR.plusYears(4);
  public static final Forelder FAR = henteForelder(Forelderrolle.FAR);
  public static final NavnDto NAVN_FAR =  NavnDto.builder().fornavn("Ronald").etternavn("McDonald").build();
  public static final Forelder MOR = henteForelder(Forelderrolle.MOR);
  public static final NavnDto NAVN_MOR = NavnDto.builder().fornavn("Ronaldina").etternavn("McDonald").build();

  public static String lageUrl(String kontekst) {
    return "http://localhost:8096" + kontekst;
  }

  public static Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument = Dokument.builder().navn("farskapserklaering.pdf")
        .signeringsinformasjonMor(Signeringsinformasjon.builder().redirectUrl(lageUrl("redirect-mor")).build())
        .signeringsinformasjonFar(Signeringsinformasjon.builder().redirectUrl(lageUrl("/redirect-far")).build())
        .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }

  public static Barn henteBarnUtenFnr(int antallUkerTilTermindato) {
    var termindato = LocalDate.now().plusDays(7 * antallUkerTilTermindato);
    return Barn.builder().termindato(termindato).build();
  }

  public static Barn henteNyligFoedtBarn() {
    var personnummer = "12340";
    var foedselsdato = LocalDate.now().minusMonths(2).minusDays(13);
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
          .foedselsnummer(FOEDSELSDATO_MOR.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerMor).build();
    } else {
      var personnummerFar = "12345";
      return Forelder.builder()
          .foedselsnummer(FOEDSELSDATO_FAR.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar).build();
    }
  }
}
