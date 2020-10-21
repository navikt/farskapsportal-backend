package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Kjoenn;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.exception.FeilKjoennPaaOppgittFarException;
import no.nav.farskapsportal.exception.OppgittNavnStemmerIkkeMedRegistrertNavnException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("FarskapsportalService")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class FarskapsportalServiceTest {

  @MockBean private PdlApiConsumer pdlApiConsumerMock;

  @Autowired private FarskapsportalService farskapsportalService;

  @Nested
  @DisplayName("henteKjoenn")
  class HenteKjoenn {

    @Test
    @DisplayName("Skal hente kjoenn for eksisterende person")
    void skalHenteKjoennForEksisterendePerson() {

      // given
      var foedselsnummerMor = "01018912345";
      when(pdlApiConsumerMock.henteKjoenn(foedselsnummerMor))
          .thenReturn(HttpResponse.from(HttpStatus.OK, Kjoenn.KVINNE));

      // when
      var respons = farskapsportalService.henteKjoenn(foedselsnummerMor);

      // then
      var kjoennReturnert = respons.getResponseEntity().getBody();
      assertAll(
          () -> assertTrue(respons.is2xxSuccessful()),
          () -> assertEquals(Kjoenn.KVINNE, kjoennReturnert));
    }

    @Test
    @DisplayName("Skal gi httpstatuskode 404 dersom informasjon om person mangler")
    void skalGiHttpstatuskode404DersomInformasjonOmPersonMangler() {

      // given
      var foedselsnummerMor = "01018912345";
      when(pdlApiConsumerMock.henteKjoenn(foedselsnummerMor))
          .thenReturn(HttpResponse.from(HttpStatus.NOT_FOUND, Kjoenn.KVINNE));

      // when
      var respons = farskapsportalService.henteKjoenn(foedselsnummerMor);

      // then
      assertTrue(respons.getResponseEntity().getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("Skal gi bad request dersom feil navn er oppgitt")
    void skalGiBadRequestDersomFeilNavnErOppgitt() {

      // given
      var request =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer("01018512340")
              .navn("Charlie Sheen")
              .build();

      var navnDto = NavnDto.builder().fornavn("Tom").etternavn("Jones").build();

      when(pdlApiConsumerMock.henteKjoenn(request.getFoedselsnummer()))
          .thenReturn(HttpResponse.from(HttpStatus.OK, Kjoenn.MANN));

      when(pdlApiConsumerMock.hentNavnTilPerson(request.getFoedselsnummer()))
          .thenReturn(HttpResponse.from(HttpStatus.OK, navnDto));

      // when, then
      assertThrows(
          OppgittNavnStemmerIkkeMedRegistrertNavnException.class,
          () -> farskapsportalService.riktigNavnOgKjoennOppgittForFar(request));
    }

    @Test
    @DisplayName("Skal gi httpstatuskode OK dersom oppgitt navn stemmer med navn i Folkeregisteret")
    void skalGiOkDersomOppgittNavnStemmerMedNavnIFolkeregisteret() {

      // given
      var request =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer("01018512340")
              .navn("Ole Idolet Brum")
              .build();

      var navnIFolkeregisteret =
          NavnDto.builder().fornavn("Ole").mellomnavn("Idolet").etternavn("Brum").build();

      when(pdlApiConsumerMock.henteKjoenn(request.getFoedselsnummer()))
          .thenReturn(HttpResponse.from(HttpStatus.OK, Kjoenn.MANN));

      when(pdlApiConsumerMock.hentNavnTilPerson(request.getFoedselsnummer()))
          .thenReturn(HttpResponse.from(HttpStatus.OK, navnIFolkeregisteret));

      // when
      var respons = farskapsportalService.riktigNavnOgKjoennOppgittForFar(request);

      // then
      assertAll(() -> assertTrue(respons.getResponseEntity().getStatusCode().is2xxSuccessful()));
    }

    @Test
    @DisplayName("Kontroll av navn skal gi OK selv om navn oppgis med små bokstaver")
    void kontrollAvNavnSkalGiOkSelvOmNavnOppgisMedSmaaBokstaver() {

      // given
      var request =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer("01018512340")
              .navn("tom richard jones")
              .build();

      var navnIFolkeregisteret =
          NavnDto.builder().fornavn("TOM").mellomnavn("RICHARD").etternavn("JONES").build();

      when(pdlApiConsumerMock.henteKjoenn(request.getFoedselsnummer()))
          .thenReturn(HttpResponse.from(HttpStatus.OK, Kjoenn.MANN));

      when(pdlApiConsumerMock.hentNavnTilPerson(request.getFoedselsnummer()))
          .thenReturn(HttpResponse.from(HttpStatus.OK, navnIFolkeregisteret));

      // when
      var respons = farskapsportalService.riktigNavnOgKjoennOppgittForFar(request);

      // then
      assertAll(() -> assertTrue(respons.getResponseEntity().getStatusCode().is2xxSuccessful()));
    }

    @Nested
    @DisplayName("RiktigNavnOgKjoennOppgittForFar")
    class RiktigNavnOgKjoennOppgittForFar {

      @Test
      @DisplayName("Skal gi bad request dersom oppgitt far ikke er mann")
      void skalGiBadRequestDersomOppgittFarIkkeErMann() {

        // given
        var request =
            KontrollerePersonopplysningerRequest.builder()
                .foedselsnummer("01018512340")
                .navn("Dolly Duck")
                .build();

        var navnDto = NavnDto.builder().fornavn("Dolly").etternavn("Duck").build();

        when(pdlApiConsumerMock.henteKjoenn(request.getFoedselsnummer()))
            .thenReturn(HttpResponse.from(HttpStatus.OK, Kjoenn.KVINNE));

        when(pdlApiConsumerMock.hentNavnTilPerson(request.getFoedselsnummer()))
            .thenReturn(HttpResponse.from(HttpStatus.NOT_FOUND, navnDto));

        // when, then
        assertThrows(
            FeilKjoennPaaOppgittFarException.class,
            () -> farskapsportalService.riktigNavnOgKjoennOppgittForFar(request));
      }
    }
  }
}
