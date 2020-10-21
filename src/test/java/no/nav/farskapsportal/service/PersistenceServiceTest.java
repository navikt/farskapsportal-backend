package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.RedirectUrlDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("PersistenceServiceTest")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class PersistenceServiceTest {

  @Autowired private PersistenceService persistenceService;

  @Nested
  @DisplayName("Tester lagreFarskapserklaering")
  class LagreFarskapserklaering {

    @Test
    @DisplayName("Lagre barn")
    void lagreBarn() {
      var termindato = LocalDate.now().plusMonths(5);
      var barn = BarnDto.builder().termindato(termindato).build();
      persistenceService.lagreBarn(barn);
      var barnReturnert = persistenceService.henteBarn(1);

      assertEquals(barn.getTermindato(), barnReturnert.getTermindato());
    }

    @Test
    @DisplayName("Lagre forelder")
    void lagreForelder() {
      // given
      var personnummerMor = "12340";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);

      var foedselsnummerMor =
          foedselsdato.plusYears(4).format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerMor;

      var mor =
          ForelderDto.builder()
              .foedselsnummer(foedselsnummerMor)
              .fornavn("Ronaldina")
              .etternavn("McDonald")
              .forelderRolle(Forelderrolle.MOR)
              .build();

      // when
      persistenceService.lagreForelder(mor);

      var forelder = persistenceService.henteForelder(1);

      assertEquals(mor.getFoedselsnummer(), forelder.getFoedselsnummer());
    }

    @Test
    @DisplayName("Lagre redirectUrl")
    void lagreRedirectUrl() throws URISyntaxException {
      // given
      var personnummerMor = "12340";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);

      var foedselsnummerMor =
          foedselsdato.plusYears(4).format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerMor;

      var mor =
          ForelderDto.builder()
              .foedselsnummer(foedselsnummerMor)
              .fornavn("Ronaldina")
              .etternavn("McDonald")
              .forelderRolle(Forelderrolle.MOR)
              .build();

      var redirectUrlDto =
          RedirectUrlDto.builder().redirectUrl(new URI("123")).signerer(mor).build();

      persistenceService.lagreRedirectUrl(redirectUrlDto);

      var retur = persistenceService.henteRedirectUrl(1);

      assertEquals(
          redirectUrlDto.getSignerer().getFoedselsnummer(),
          retur.getSignerer().getFoedselsnummer());
    }

    @Test
    @DisplayName("Lagre dokument")
    void lagreDokument() throws URISyntaxException {
      var personnummerFar = "12345";
      var personnummerMor = "12340";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummerFar =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar;
      var foedselsnummerMor =
          foedselsdato.plusYears(4).format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerMor;

      var termindato = LocalDate.now().plusMonths(5);

      var mor =
          ForelderDto.builder()
              .foedselsnummer(foedselsnummerMor)
              .fornavn("Ronaldina")
              .etternavn("McDonald")
              .forelderRolle(Forelderrolle.MOR)
              .build();

      var far =
          ForelderDto.builder()
              .foedselsnummer(foedselsnummerFar)
              .fornavn("Ronald")
              .etternavn("McDonald")
              .forelderRolle(Forelderrolle.FAR)
              .build();

      var redirectUrlMor = RedirectUrlDto.builder().redirectUrl(new URI("")).signerer(mor).build();
      var redirectUrlFar = RedirectUrlDto.builder().redirectUrl(new URI("")).signerer(far).build();

      var dokument =
          DokumentDto.builder()
              .dokumentnavn("farskapserklaring.pdf")
              .padesUrl(new URI(""))
              .dokumentRedirectMor(redirectUrlMor)
              .dokumentRedirectFar(redirectUrlFar)
              .build();

      // when
      persistenceService.lagreDokument(dokument);

      var retur = persistenceService.henteDokument(1);
      assertEquals(dokument.getDokumentRedirectFar().getSignerer().getFoedselsnummer(), retur.getDokumentRedirectFar().getSignerer().getFoedselsnummer());
    }

    @Test
    @DisplayName("Lagre farskapserklæring")
    void lagreFarskapserklaering() throws URISyntaxException {

      // given
      var personnummerFar = "12345";
      var personnummerMor = "12340";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummerFar =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar;
      var foedselsnummerMor =
          foedselsdato.plusYears(4).format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerMor;

      var termindato = LocalDate.now().plusMonths(5);

      var barn = BarnDto.builder().termindato(termindato).build();

      var mor =
          ForelderDto.builder()
              .foedselsnummer(foedselsnummerMor)
              .fornavn("Ronaldina")
              .etternavn("McDonald")
              .forelderRolle(Forelderrolle.MOR)
              .build();

      var far =
          ForelderDto.builder()
              .foedselsnummer(foedselsnummerFar)
              .fornavn("Ronald")
              .etternavn("McDonald")
              .forelderRolle(Forelderrolle.FAR)
              .build();

      var redirectUrlMor = RedirectUrlDto.builder().redirectUrl(new URI("")).signerer(mor).build();
      var redirectUrlFar = RedirectUrlDto.builder().redirectUrl(new URI("")).signerer(far).build();

      var dokument =
          DokumentDto.builder()
              .dokumentnavn("farskapserklaering.pdf")
              .padesUrl(new URI(""))
              .dokumentRedirectMor(redirectUrlMor)
              .dokumentRedirectFar(redirectUrlFar)
              .build();

      var farskapserklaering =
          FarskapserklaeringDto.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();

      // when
      persistenceService.lagreFarskapserklaering(farskapserklaering);

      var fnr = persistenceService.henteFarskapserklaeringer(foedselsnummerFar);

      // then
      assertEquals(foedselsnummerFar, fnr);
    }
  }
}
