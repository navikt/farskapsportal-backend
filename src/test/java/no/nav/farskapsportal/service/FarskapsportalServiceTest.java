package no.nav.farskapsportal.service;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.api.KontrollerePersonopplysningerRequest;
import no.nav.farskapsportal.api.OppretteFarskaperklaeringRequest;
import no.nav.farskapsportal.consumer.esignering.DifiESignaturConsumer;
import no.nav.farskapsportal.consumer.pdf.PdfGeneratorConsumer;
import no.nav.farskapsportal.consumer.pdl.api.KjoennDto;
import no.nav.farskapsportal.consumer.pdl.api.KjoennTypeDto;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.DokumentStatusDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.RedirectUrlDto;
import no.nav.farskapsportal.dto.SignaturDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("FarskapserklaeringService")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class FarskapsportalServiceTest {

  @MockBean PdfGeneratorConsumer pdfGeneratorConsumer;
  @MockBean DifiESignaturConsumer difiESignaturConsumer;
  @MockBean PersistenceService persistenceService;
  @MockBean PersonopplysningService personopplysningService;

  @Autowired private FarskapsportalService farskapsportalService;

  @Nested
  @DisplayName("Teste henteBrukerinformasjon")
  class HenteBrukerinformasjon {

    @Test
    @DisplayName("Skal returnere påbegynte farskapserklæringer som venter på far")
    void skalReturnerePaabegynteFarskapserklaeringerSomVenterPaaFar() {

      // given
      var personnummerFar = "12345";
      var foedselsdato = LocalDate.now().minusYears(35).minusMonths(2).minusDays(13);
      var foedselsnummerFar =
          foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummerFar;

      var far =
          ForelderDto.builder()
              .foedselsnummer(foedselsnummerFar)
              .fornavn("Ronald")
              .etternavn("McDonald")
              .forelderRolle(Forelderrolle.FAR)
              .build();

      var farskapserklaeringSomVenterPaaFarsSignatur =
          FarskapserklaeringDto.builder().far(far).build();

      when(personopplysningService.bestemmeForelderrolle(far.getFoedselsnummer()))
          .thenReturn(far.getForelderRolle());
      when(persistenceService.henteFarskapserklaeringerEtterRedirect(
              far.getFoedselsnummer(), far.getForelderRolle(), KjoennTypeDto.MANN))
          .thenReturn(Set.of(farskapserklaeringSomVenterPaaFarsSignatur));
      when(persistenceService.henteFarskapserklaeringer(far.getFoedselsnummer()))
          .thenReturn(Set.of(farskapserklaeringSomVenterPaaFarsSignatur));

      // when
      var brukerinformasjon = farskapsportalService.henteBrukerinformasjon(far.getFoedselsnummer());

      // then
      assertTrue(
          brukerinformasjon
              .getFarsVentendeFarskapserklaeringer()
              .containsAll(Set.of(farskapserklaeringSomVenterPaaFarsSignatur)));
    }
  }

  @Nested
  @DisplayName("Teste oppretteFarskapserklaering")
  class OppretteFarskapserklaering {

    @SneakyThrows
    @Test
    @DisplayName("Skal opprette farskapserklæring for barn med termindato")
    void skalOppretteFarskapserklaeringForBarnMedTermindato() {

      // given
      var fnrMor = "11111112340";
      var fnrFar = "00000012121";
      var barn = BarnDto.builder().termindato(LocalDate.now().plusMonths(5)).build();
      var registrertNavnMor = NavnDto.builder().fornavn("Natalya").etternavn("Sagdiyev").build();
      var registrertNavnFar = NavnDto.builder().fornavn("Jessie").etternavn("James").build();
      var opplysningerOmFar =
          KontrollerePersonopplysningerRequest.builder()
              .foedselsnummer(fnrFar)
              .navn(registrertNavnFar.getFornavn() + " " + registrertNavnFar.getEtternavn())
              .build();

      var pdf =
          DokumentDto.builder()
              .dokumentnavn("Farskapserklæering.pdf")
              .innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
              .redirectUrlMor(
                  RedirectUrlDto.builder()
                      .redirectUrl(new URI("https://til-signering-mor.no/"))
                      .signerer(
                          ForelderDto.builder()
                              .fornavn(registrertNavnMor.getFornavn())
                              .etternavn(registrertNavnMor.getEtternavn())
                              .forelderRolle(Forelderrolle.MOR)
                              .foedselsnummer(fnrMor)
                              .build())
                      .build())
              .build();

      when(personopplysningService.henteNavn(fnrMor)).thenReturn(registrertNavnMor);
      when(personopplysningService.henteNavn(fnrFar)).thenReturn(registrertNavnFar);
      when(pdfGeneratorConsumer.genererePdf(any())).thenReturn(pdf);
      doNothing().when(difiESignaturConsumer).oppretteSigneringsjobb(any(), any(), any());
      when(persistenceService.lagreFarskapserklaering(any())).thenReturn(null);

      // when
      var respons =
          farskapsportalService.oppretteFarskapserklaering(
              fnrMor,
              OppretteFarskaperklaeringRequest.builder()
                  .barn(barn)
                  .opplysningerOmFar(opplysningerOmFar)
                  .build());

      // then
      assertEquals(
          pdf.getRedirectUrlMor().getRedirectUrl(), respons.getRedirectUrlForSigneringMor());
    }
  }

  @Nested
  @DisplayName("Teste henteSignertDokumentEtterRedirect")
  class HenteSignertDokumentEtterRedirect {

    @SneakyThrows
    @Test
    @DisplayName("Skal hente dokument etter redirect dersom status query token er gyldig")
    void skalHenteDokumentEtterRedirectDersomStatusQueryTokenErGyldig() {
      // given
      var registrertNavnFar = NavnDto.builder().fornavn("Jessie").etternavn("James").build();
      var far =
          ForelderDto.builder()
              .foedselsnummer("00001122111")
              .fornavn(registrertNavnFar.getFornavn())
              .etternavn(registrertNavnFar.getEtternavn())
              .forelderRolle(Forelderrolle.FAR)
              .build();

      var mor =
          ForelderDto.builder()
              .foedselsnummer("11001122110")
              .fornavn("Dolly")
              .etternavn("Duck")
              .build();

      var statuslenke = "https://hvaskjera.no/";

      var farskapserklaering =
          FarskapserklaeringDto.builder()
              .mor(mor)
              .far(far)
              .dokument(
                  DokumentDto.builder()
                      .redirectUrlFar(
                          RedirectUrlDto.builder()
                              .redirectUrl(new URI("https://take-me-to-the-doc.no/"))
                              .build())
                      .innhold("Jeg erklærer herved farskap til dette barnet..".getBytes())
                      .dokumentnavn("farskapserklæring.pdf")
                      .dokumentStatusUrl(new URI(statuslenke))
                      .build())
              .build();

      when(personopplysningService.bestemmeForelderrolle(far.getFoedselsnummer()))
          .thenReturn(far.getForelderRolle());
      when(personopplysningService.henteGjeldendeKjoenn(far.getFoedselsnummer()))
          .thenReturn(KjoennDto.builder().kjoenn(KjoennTypeDto.MANN).build());

      when(persistenceService.henteFarskapserklaeringerEtterRedirect(
              far.getFoedselsnummer(), Forelderrolle.FAR, KjoennTypeDto.MANN))
          .thenReturn(Set.of(farskapserklaering));

      when(persistenceService.lagreFarskapserklaering(any())).thenReturn(null);

      when(difiESignaturConsumer.henteDokumentstatusEtterRedirect(any(), any()))
          .thenReturn(
              DokumentStatusDto.builder()
                  .statuslenke(new URI(statuslenke))
                  .erSigneringsjobbenFerdig(true)
                  .padeslenke(new URI("https://permanent-pades-url.no/"))
                  .signaturer(
                      List.of(
                          SignaturDto.builder()
                              .signatureier(far.getFoedselsnummer())
                              .harSignert(true)
                              .tidspunktForSignering(LocalDateTime.now().minusSeconds(3))
                              .build()))
                  .build());

      when(difiESignaturConsumer.henteSignertDokument(any()))
          .thenReturn(farskapserklaering.getDokument().getInnhold());

      // when
      var respons =
          farskapsportalService.henteSignertDokumentEtterRedirect(
              far.getFoedselsnummer(), "etGyldigStatusQueryToken");

      // then
      assertEquals(farskapserklaering.getDokument().getInnhold(), respons);
    }
  }
}
