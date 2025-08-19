package no.nav.farskapsportal.backend.apps.api.service;

import java.net.URI;
import java.net.URISyntaxException;
import no.nav.farskapsportal.backend.libs.dto.BarnDto;
import no.nav.farskapsportal.backend.libs.dto.DokumentDto;
import no.nav.farskapsportal.backend.libs.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.backend.libs.dto.ForelderDto;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.dto.StatusKontrollereFarDto;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.StatusKontrollereFar;
import no.nav.farskapsportal.backend.libs.felles.exception.MappingException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Mapper {

  @Autowired private ModelMapper modelMapper;

  @Autowired private PersonopplysningService personopplysningService;

  public Barn toEntity(BarnDto barnDto) {
    return modelMapper.map(barnDto, Barn.class);
  }

  public BarnDto toDto(Barn barn) {
    return modelMapper.map(barn, BarnDto.class);
  }

  public ForelderDto toDto(Forelder forelder) {
    var foedselsdato = personopplysningService.henteFødselsdato(forelder.getFoedselsnummer());
    var navnDto =
        modelMapper(personopplysningService.henteNavn(forelder.getFoedselsnummer()), NavnDto.class);
    var forelderDto = modelMapper.map(forelder, ForelderDto.class);
    forelderDto.setFoedselsdato(foedselsdato);
    forelderDto.setNavn(navnDto);
    return forelderDto;
  }

  public DokumentDto toDto(Dokument dokument) {

    try {
      return DokumentDto.builder()
          .dokumentnavn(dokument.getNavn())
          .redirectUrlFar(
              dokument.getSigneringsinformasjonFar().getRedirectUrl() != null
                  ? new URI(dokument.getSigneringsinformasjonFar().getRedirectUrl())
                  : null)
          .redirectUrlMor(
              dokument.getSigneringsinformasjonMor().getRedirectUrl() != null
                  ? new URI(dokument.getSigneringsinformasjonMor().getRedirectUrl())
                  : null)
          .signertAvMor(dokument.getSigneringsinformasjonMor().getSigneringstidspunkt())
          .signertAvFar(dokument.getSigneringsinformasjonFar().getSigneringstidspunkt())
          .redirectUrlFar(
              dokument.getSigneringsinformasjonFar().getRedirectUrl() != null
                  ? new URI(dokument.getSigneringsinformasjonFar().getRedirectUrl())
                  : null)
          .build();

    } catch (URISyntaxException uriSyntaxException) {
      throw new MappingException("Mapping dokument entitet til Dto feilet", uriSyntaxException);
    }
  }

  public Forelder toEntity(ForelderDto forelderDto) {
    return modelMapper.map(forelderDto, Forelder.class);
  }

  public <T, V> T modelMapper(V v, Class<T> tClass) {
    return modelMapper.map(v, tClass);
  }

  public FarskapserklaeringDto toDto(Farskapserklaering farskapserklaering) {
    var dokumentDto = toDto(farskapserklaering.getDokument());
    var forelderDtoMor = toDto(farskapserklaering.getMor());
    var forelderDtoFar = toDto(farskapserklaering.getFar());
    var farskapserklaeringDto = modelMapper.map(farskapserklaering, FarskapserklaeringDto.class);
    farskapserklaeringDto.setDokument(dokumentDto);
    farskapserklaeringDto.setFar(forelderDtoFar);
    farskapserklaeringDto.setMor(forelderDtoMor);

    return farskapserklaeringDto;
  }

  public StatusKontrollereFar toEntity(StatusKontrollereFarDto statusKontrollereFarDto) {
    var mor = toEntity(statusKontrollereFarDto.getMor());
    return StatusKontrollereFar.builder()
        .mor(mor)
        .tidspunktForNullstilling(statusKontrollereFarDto.getTidspunktForNullstilling())
        .antallFeiledeForsoek(statusKontrollereFarDto.getAntallFeiledeForsoek())
        .build();
  }

  public StatusKontrollereFarDto toDto(StatusKontrollereFar statusKontrollereFar) {
    var morDto = toDto(statusKontrollereFar.getMor());
    return StatusKontrollereFarDto.builder()
        .mor(morDto)
        .tidspunktForNullstilling(statusKontrollereFar.getTidspunktForNullstilling())
        .antallFeiledeForsoek(statusKontrollereFar.getAntallFeiledeForsoek())
        .build();
  }
}
