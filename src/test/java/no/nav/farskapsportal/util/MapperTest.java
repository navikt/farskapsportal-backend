package no.nav.farskapsportal.util;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.StatusKontrollereFarDto;
import no.nav.farskapsportal.persistence.entity.Barn;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.StatusKontrollereFar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("MappingUtilTest")
@SpringBootTest(classes = {Mapper.class, ModelMapper.class})
@ActiveProfiles(PROFILE_TEST)
public class MapperTest {

  private static final ForelderDto MOR_DTO = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR_DTO = henteForelder(Forelderrolle.FAR);
  private static final DokumentDto DOKUMENT_DTO = getDokumentDto();
  private static final LocalDate TERMINDATO = LocalDate.now().plusMonths(2).minusDays(13);

  @Autowired
  private Mapper mapper;

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
      assertAll(() -> assertEquals(FAR_DTO.getFoedselsnummer(), forelder.getFoedselsnummer()),
          () -> assertEquals(FAR_DTO.getFornavn(), forelder.getFornavn()), () -> assertEquals(FAR_DTO.getEtternavn(), forelder.getEtternavn()));

    }

    @Test
    @DisplayName("Skal mappe forelder, entitet til DTO")
    void skalMappeForelderEntitetTilDto() {

      // given
      var forelder = Forelder.builder().fornavn("Sponge").etternavn("Bob").foedselsnummer("12345678910").build();

      // when
      var forelderDto = mapper.toDto(forelder);

      // then
      assertAll(() -> assertEquals(forelder.getFoedselsnummer(), forelderDto.getFoedselsnummer()),
          () -> assertEquals(forelder.getFornavn(), forelderDto.getFornavn()),
          () -> assertEquals(forelder.getEtternavn(), forelderDto.getEtternavn()));

    }
  }

  @Nested
  @DisplayName("Skal mappe mellom DTO og entitet for Dokument")
  class DokumentMapping {

    @Test
    @DisplayName("Skal mappe dokument, DTO til entitet")
    void skalMappeDokumentDtoTilEntitet() {

      // given, when
      var dokument = mapper.toEntity(DOKUMENT_DTO);

      // then
      assertAll(() -> assertEquals(DOKUMENT_DTO.getDokumentnavn(), dokument.getDokumentnavn()),
          () -> assertEquals(DOKUMENT_DTO.getSignertAvFar(), dokument.getSigneringsinformasjonFar().getSigneringstidspunkt()),
          () -> assertEquals(DOKUMENT_DTO.getSignertAvMor(), dokument.getSigneringsinformasjonMor().getSigneringstidspunkt()),
          () -> assertEquals(DOKUMENT_DTO.getRedirectUrlFar().toString(), dokument.getSigneringsinformasjonFar().getRedirectUrl()),
          () -> assertEquals(DOKUMENT_DTO.getRedirectUrlMor().toString(), dokument.getSigneringsinformasjonMor().getRedirectUrl()));
    }

    @Test
    @DisplayName("Skal mappe dokument, entitet til DTO")
    void skalMappeDokumentEntitetTilDto() {

      // given
      var dokument = mapper.toEntity(DOKUMENT_DTO);

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
    @DisplayName("Skal mappe farskapserklaering, DTO til entitet")
    void skalMappeFarskapserklaeringDtoTilEntitet() {

      // given
      var farskapserklaeringDto = FarskapserklaeringDto.builder().far(FAR_DTO).mor(MOR_DTO).barn(BarnDto.builder().termindato(TERMINDATO).build())
          .dokument(DOKUMENT_DTO).build();

      // when
      var farskapserklaering = mapper.toEntity(farskapserklaeringDto);

      // then
      assertAll(() -> assertEquals(FAR_DTO.getFoedselsnummer(), farskapserklaering.getFar().getFoedselsnummer()),
          () -> assertEquals(MOR_DTO.getFoedselsnummer(), farskapserklaering.getMor().getFoedselsnummer()),
          () -> assertEquals(TERMINDATO, farskapserklaering.getBarn().getTermindato())
      );
    }

    @Test
    @DisplayName("Skal mappe farskapserklaering - Entitet til DTO")
    void skalMappeFarskapserklaeringEntitetTilDto() {

      // given
      var dokument = mapper.toEntity(DOKUMENT_DTO);
      var far = mapper.toEntity(FAR_DTO);
      var mor = mapper.toEntity(MOR_DTO);
      var farskapserklaering = Farskapserklaering.builder().far(far).mor(mor).dokument(dokument).barn(Barn.builder().termindato(TERMINDATO).build())
          .build();

      // when
      var farskapserklaeringDto = mapper.toDto(farskapserklaering);

      // then
      assertAll(() -> assertEquals(FAR_DTO.getFoedselsnummer(), farskapserklaeringDto.getFar().getFoedselsnummer()),
          () -> assertEquals(MOR_DTO.getFoedselsnummer(), farskapserklaeringDto.getMor().getFoedselsnummer()),
          () -> assertEquals(TERMINDATO, farskapserklaeringDto.getBarn().getTermindato()),
          () -> assertEquals(farskapserklaering.getDokument().getSigneringsinformasjonMor().getSigneringstidspunkt(),
              farskapserklaeringDto.getDokument().getSignertAvMor()));
    }

    @Test
    @DisplayName("Skal mappe farskapserklaering som er send til Skatt - Entitet til DTO")
    void skalMappeFarskapserklaeringSomErSendtTilSkattEntitetTilDto() {

      // given
      var dokument = mapper.toEntity(DOKUMENT_DTO);
      var far = mapper.toEntity(FAR_DTO);
      var mor = mapper.toEntity(MOR_DTO);
      var farskapserklaering = Farskapserklaering.builder().far(far).mor(mor).dokument(dokument).barn(Barn.builder().termindato(TERMINDATO).build())
          .meldingsidSkatt("123444").sendtTilSkatt(LocalDateTime.now().minusDays(3))
          .build();

      // when
      var farskapserklaeringDto = mapper.toDto(farskapserklaering);

      // then
      assertAll(
          () -> assertEquals(FAR_DTO.getFoedselsnummer(), farskapserklaeringDto.getFar().getFoedselsnummer()),
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
      var tidspunktSisteFeiledeForsoek = LocalDateTime.now();
      var antallFeiledeForsoek = 3;
      var mor = mapper.toEntity(MOR_DTO);
      var entitet = StatusKontrollereFar.builder().mor(mor).tidspunktSisteFeiledeForsoek(tidspunktSisteFeiledeForsoek)
          .antallFeiledeForsoek(antallFeiledeForsoek).build();

      // when
      var dto = mapper.toDto(entitet);

      // then
      assertAll(() -> assertThat(MOR_DTO.getFoedselsnummer()).isEqualTo(dto.getMor().getFoedselsnummer()),
          () -> assertThat(tidspunktSisteFeiledeForsoek).isEqualTo(dto.getTidspunktSisteFeiledeForsoek()),
          () -> assertThat(antallFeiledeForsoek).isEqualTo(dto.getAntallFeiledeForsoek()));
    }

    @Test
    @DisplayName("Skal mappe StatusKontrollereFar - DTO til entitet")
    void skalMappeStatusKontrollereFarDtoTilEntitet() {

      // given
      var tidspunktSisteFeiledeForsoek = LocalDateTime.now();
      var antallFeiledeForsoek = 3;
      var dto = StatusKontrollereFarDto.builder().mor(MOR_DTO).tidspunktSisteFeiledeForsoek(tidspunktSisteFeiledeForsoek)
          .antallFeiledeForsoek(antallFeiledeForsoek).build();

      // when
      var entitet = mapper.toEntity(dto);

      // then
      assertAll(() -> assertThat(MOR_DTO.getFoedselsnummer()).isEqualTo(entitet.getMor().getFoedselsnummer()),
          () -> assertThat(tidspunktSisteFeiledeForsoek).isEqualTo(entitet.getTidspunktSisteFeiledeForsoek()),
          () -> assertThat(antallFeiledeForsoek).isEqualTo(entitet.getAntallFeiledeForsoek()));
    }
  }
}
