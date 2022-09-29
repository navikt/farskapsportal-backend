package no.nav.farskapsportal.backend.apps.api.service;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FOEDSELSDATO_MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.libs.dto.BarnDto;
import no.nav.farskapsportal.backend.libs.dto.DokumentDto;
import no.nav.farskapsportal.backend.libs.dto.ForelderDto;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.dto.StatusKontrollereFarDto;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.entity.StatusKontrollereFar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("MapperTest")
@SpringBootTest(classes = FarskapsportalApiApplicationLocal.class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles(PROFILE_TEST)
public class MapperTest {

  private static final Forelder MOR = henteForelder(Forelderrolle.MOR);
  private static final NavnDto NAVN_MOR = NavnDto.builder().fornavn("Dolly").etternavn("Duck").build();
  private static final ForelderDto MOR_DTO = ForelderDto.builder()
      .forelderrolle(Forelderrolle.MOR).navn(NAVN_MOR).foedselsdato(FOEDSELSDATO_MOR).foedselsnummer(MOR.getFoedselsnummer()).build();

  private static final Forelder FAR = henteForelder(Forelderrolle.FAR);
  private static final NavnDto NAVN_FAR = NavnDto.builder().fornavn("Fetter").etternavn("Anton").build();
  private static final ForelderDto FAR_DTO = ForelderDto.builder()
      .forelderrolle(Forelderrolle.FAR).foedselsnummer(FAR.getFoedselsnummer()).foedselsdato(FOEDSELSDATO_FAR).navn(NAVN_FAR).build();

  private static final DokumentDto DOKUMENT_DTO = getDokumentDto();
  private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(2).minusDays(13);

  @Autowired
  private Mapper mapper;

  @MockBean
  private PersonopplysningService personopplysningService;

  private static DokumentDto getDokumentDto() {
    try {
      return DokumentDto.builder().dokumentnavn("Farskapserklæring.pdf")
          .redirectUrlFar(new URI("https://redirectUrlFar.posten.no/"))
          .redirectUrlMor(new URI("https://redirectUrlMor.posten.no/"))
          .signertAvMor(LocalDateTime.now()).signertAvFar(LocalDateTime.now()).build();
    } catch (URISyntaxException uriSyntaxException) {
      uriSyntaxException.printStackTrace();
    }

    return null;
  }

  private void standardPersonopplysningerMocks(Forelder far, Forelder mor) {
    when(personopplysningService.henteNavn(far.getFoedselsnummer())).thenReturn(NAVN_FAR);
    when(personopplysningService.henteFoedselsdato(far.getFoedselsnummer())).thenReturn(FOEDSELSDATO_FAR);

    when(personopplysningService.henteNavn(mor.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(personopplysningService.henteFoedselsdato(mor.getFoedselsnummer())).thenReturn(FOEDSELSDATO_MOR);
    when(personopplysningService.harNorskBostedsadresse(mor.getFoedselsnummer())).thenReturn(true);
  }

  @Nested
  @DisplayName("Skal mappe mellom DTO og entitet for Barn")
  class BarnMapping {

    @Test
    @DisplayName("Skal mappe barn med termindato til dto")
    void skalMappeBarnMedTermindatoTilDto() {

      // given
      var barn = Barn.builder().termindato(TERMINDATO).build();

      // when
      var barnDto = mapper.toDto(barn);

      // then
      assertEquals(TERMINDATO, barnDto.getTermindato());
    }

    @Test
    @DisplayName("Skal mappe barn med fødselsnummer til dto")
    void skalMappeBarnMedFoedselsnummerTilDto() {

      // given
      var foedselsdato = LocalDate.now().minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + "12345";
      var barn = Barn.builder().foedselsnummer(foedselsnummer).build();

      // when
      var barnDto = mapper.toDto(barn);

      // then
      assertEquals(foedselsnummer, barnDto.getFoedselsnummer());
    }


    @Test
    @DisplayName("Skal mappe barnDto med termindato til entitet")
    void skalMappeBarnDtoMedTermindatoTilEntitiet() {

      // given
      var barnDto = BarnDto.builder().termindato(TERMINDATO).build();

      // when
      var barn = mapper.toEntity(barnDto);

      // then
      assertEquals(TERMINDATO, barn.getTermindato());
    }

    @Test
    @DisplayName("Skal mappe barnDto med fødselsnummer til entitet")
    void skalMappeBarnDtoMedFoedselsnummerTilEntitiet() {

      // given
      var foedselsdato = LocalDate.now().minusMonths(2).minusDays(13);
      var foedselsnummer = foedselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + "12345";
      var barnDto = BarnDto.builder().foedselsnummer(foedselsnummer).build();

      // when
      var barn = mapper.toEntity(barnDto);

      // then
      assertEquals(barnDto.getFoedselsnummer(), barn.getFoedselsnummer());
    }
  }

  @Nested
  @DisplayName("Skal mappe mellom DTO og entitet for Forelder")
  class ForelderMapping {

    @Test
    @DisplayName("Skal mappe forelder, DTO til entitet")
    void skalMappeForelderDtoTilEntitet() {

      // given, when
      var forelder = mapper.toEntity(FAR_DTO);

      // then
      assertEquals(FAR.getFoedselsnummer(), forelder.getFoedselsnummer());
    }

    @Test
    @DisplayName("Skal mappe forelder, entitet til DTO")
    void skalMappeForelderEntitetTilDto() {

      String fnrForelder = "12345678910";

      // given
      var forelder = Forelder.builder().foedselsnummer(fnrForelder).build();

      when(personopplysningService.henteNavn(fnrForelder)).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFoedselsdato(fnrForelder)).thenReturn(MOR_DTO.getFoedselsdato());

      when(personopplysningService.harNorskBostedsadresse(fnrForelder)).thenReturn(true);

      // when
      var forelderDto = mapper.toDto(forelder);

      // then
      assertEquals(forelder.getFoedselsnummer(), forelderDto.getFoedselsnummer());
    }
  }

  @Nested
  @DisplayName("Skal mappe mellom DTO og entitet for Dokument")
  class DokumentMapping {


    @Test
    @DisplayName("Skal mappe dokument, entitet til DTO")
    void skalMappeDokumentEntitetTilDto() {

      // given
      var dokument = toEntity(DOKUMENT_DTO);

      // when
      var dokumentDto = mapper.toDto(dokument);

      // then
      assertAll(
          () -> assertEquals(DOKUMENT_DTO.getRedirectUrlMor(), dokumentDto.getRedirectUrlMor()),
          () -> assertEquals(DOKUMENT_DTO.getRedirectUrlFar(), dokumentDto.getRedirectUrlFar()),
          () -> assertEquals(DOKUMENT_DTO.getDokumentnavn(), dokumentDto.getDokumentnavn()),
          () -> assertEquals(DOKUMENT_DTO.getSignertAvMor(), dokumentDto.getSignertAvMor()),
          () -> assertEquals(DOKUMENT_DTO.getSignertAvFar(), dokumentDto.getSignertAvFar())
      );
    }
  }

  @Nested
  @DisplayName("Skal mappe mellom DTO og entitet for Farskapserklaering")
  class FarskapserklaeringMapping {

    @Test
    @DisplayName("Skal mappe farskapserklaering - Entitet til DTO")
    void skalMappeFarskapserklaeringEntitetTilDto() {

      // given
      var dokument = toEntity(DOKUMENT_DTO);
      var far = mapper.toEntity(FAR_DTO);
      var mor = mapper.toEntity(MOR_DTO);
      var farskapserklaering = Farskapserklaering.builder().far(far).mor(mor).dokument(dokument).barn(Barn.builder().termindato(TERMINDATO).build())
          .build();

      standardPersonopplysningerMocks(far, mor);

      // when
      var farskapserklaeringDto = mapper.toDto(farskapserklaering);

      // then
      assertAll(() -> assertEquals(FAR.getFoedselsnummer(), farskapserklaeringDto.getFar().getFoedselsnummer()),
          () -> assertEquals(MOR_DTO.getFoedselsnummer(), farskapserklaeringDto.getMor().getFoedselsnummer()),
          () -> assertEquals(TERMINDATO, farskapserklaeringDto.getBarn().getTermindato()),
          () -> assertEquals(farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt(),
              farskapserklaeringDto.getDokument().getSignertAvMor()));
    }

    @Test
    @DisplayName("Skal mappe farskapserklaering som er send til Skatt - Entitet til DTO")
    void skalMappeFarskapserklaeringSomErSendtTilSkattEntitetTilDto() {

      // given
      var dokument = toEntity(DOKUMENT_DTO);
      var far = mapper.toEntity(FAR_DTO);
      var mor = mapper.toEntity(MOR_DTO);
      var farskapserklaering = Farskapserklaering.builder().far(far).mor(mor).dokument(dokument).barn(Barn.builder().termindato(TERMINDATO).build())
          .meldingsidSkatt("123444").sendtTilSkatt(LocalDateTime.now().minusDays(3))
          .build();

      standardPersonopplysningerMocks(far, mor);

      // when
      var farskapserklaeringDto = mapper.toDto(farskapserklaering);

      // then
      assertAll(
          () -> assertEquals(FAR.getFoedselsnummer(), farskapserklaeringDto.getFar().getFoedselsnummer()),
          () -> assertEquals(MOR_DTO.getFoedselsnummer(), farskapserklaeringDto.getMor().getFoedselsnummer()),
          () -> assertEquals(TERMINDATO, farskapserklaeringDto.getBarn().getTermindato()),
          () -> assertEquals(farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt(),
              farskapserklaeringDto.getDokument().getSignertAvMor()),
          () -> assertEquals(farskapserklaeringDto.getMeldingsidSkatt(), farskapserklaering.getMeldingsidSkatt()),
          () -> assertEquals(farskapserklaeringDto.getSendtTilSkatt(), farskapserklaering.getSendtTilSkatt())
      );
    }

    @Test
    @DisplayName("Skal mappe StatusKontrollereFar - Entitet til DTO")
    void skalMappeStatusKontrollereFarEntitetTilDto() {

      // given
      var tidspunktForNullstilling = LocalDateTime.now();
      var antallFeiledeForsoek = 3;
      var mor = mapper.toEntity(MOR_DTO);
      var entitet = StatusKontrollereFar.builder().mor(mor).tidspunktForNullstilling(tidspunktForNullstilling)
          .antallFeiledeForsoek(antallFeiledeForsoek).build();

      when(personopplysningService.henteNavn(mor.getFoedselsnummer())).thenReturn(NAVN_MOR);
      when(personopplysningService.henteFoedselsdato(mor.getFoedselsnummer())).thenReturn(MOR_DTO.getFoedselsdato());
      when(personopplysningService.harNorskBostedsadresse(mor.getFoedselsnummer())).thenReturn(true);

      // when
      var dto = mapper.toDto(entitet);

      // then
      assertAll(() -> assertThat(MOR_DTO.getFoedselsnummer()).isEqualTo(dto.getMor().getFoedselsnummer()),
          () -> assertThat(tidspunktForNullstilling).isEqualTo(dto.getTidspunktForNullstilling()),
          () -> assertThat(antallFeiledeForsoek).isEqualTo(dto.getAntallFeiledeForsoek()));
    }

    @Test
    @DisplayName("Skal mappe StatusKontrollereFar - DTO til entitet")
    void skalMappeStatusKontrollereFarDtoTilEntitet() {

      // given
      var tidspunktSisteFeiledeForsoek = LocalDateTime.now();
      var antallFeiledeForsoek = 3;
      var dto = StatusKontrollereFarDto.builder().mor(MOR_DTO).tidspunktForNullstilling(tidspunktSisteFeiledeForsoek)
          .antallFeiledeForsoek(antallFeiledeForsoek).build();

      // when
      var entitet = mapper.toEntity(dto);

      // then
      assertAll(() -> assertThat(MOR_DTO.getFoedselsnummer()).isEqualTo(entitet.getMor().getFoedselsnummer()),
          () -> assertThat(tidspunktSisteFeiledeForsoek).isEqualTo(entitet.getTidspunktForNullstilling()),
          () -> assertThat(antallFeiledeForsoek).isEqualTo(entitet.getAntallFeiledeForsoek()));
    }
  }

  private Dokument toEntity(DokumentDto dokumentDto) {
    var signeringsinformasjonMor = Signeringsinformasjon.builder()
        .redirectUrl(dokumentDto.getRedirectUrlMor().toString())
        .signeringstidspunkt(dokumentDto.getSignertAvMor()).build();

    var signeringsinformasjonFar = Signeringsinformasjon.builder()
        .redirectUrl(dokumentDto.getRedirectUrlFar().toString())
        .signeringstidspunkt(dokumentDto.getSignertAvFar()).build();

    return Dokument.builder()
        .signeringsinformasjonMor(signeringsinformasjonMor)
        .signeringsinformasjonFar(signeringsinformasjonFar)
        .navn(dokumentDto.getDokumentnavn())
        .build();
  }
}
