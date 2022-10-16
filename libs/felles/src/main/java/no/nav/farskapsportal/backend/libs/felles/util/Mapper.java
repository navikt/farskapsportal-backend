package no.nav.farskapsportal.backend.libs.felles.util;

import java.net.URI;
import java.net.URISyntaxException;
import no.nav.farskapsportal.backend.libs.felles.exception.MappingException;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.entity.StatusKontrollereFar;
import no.nav.farskapsportal.backend.libs.felles.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.dto.BarnDto;
import no.nav.farskapsportal.backend.libs.dto.DokumentDto;
import no.nav.farskapsportal.backend.libs.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.backend.libs.dto.ForelderDto;
import no.nav.farskapsportal.backend.libs.dto.NavnDto;
import no.nav.farskapsportal.backend.libs.dto.StatusKontrollereFarDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Mapper {

  @Autowired
  private ModelMapper modelMapper;

  @Autowired
  private PersonopplysningService personopplysningService;

  public Barn toEntity(BarnDto barnDto) {
    return modelMapper.map(barnDto, Barn.class);
  }

  public BarnDto toDto(Barn barn) {
    return modelMapper.map(barn, BarnDto.class);
  }

  public Dokument toEntity(DokumentDto dokumentDto) {
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

  public DokumentDto toDto(Dokument dokument) {

    try {
      return DokumentDto.builder()
          .dokumentnavn(dokument.getNavn())
          .redirectUrlFar(dokument.getSigneringsinformasjonFar().getRedirectUrl() != null
              ? new URI(dokument.getSigneringsinformasjonFar().getRedirectUrl()) : null)
          .redirectUrlMor(dokument.getSigneringsinformasjonMor().getRedirectUrl() != null
              ? new URI(dokument.getSigneringsinformasjonMor().getRedirectUrl()) : null)
          .signertAvMor(dokument.getSigneringsinformasjonMor().getSigneringstidspunkt())
          .signertAvFar(dokument.getSigneringsinformasjonFar().getSigneringstidspunkt())
          .redirectUrlFar(dokument.getSigneringsinformasjonFar().getRedirectUrl() != null
              ? new URI(dokument.getSigneringsinformasjonFar().getRedirectUrl()) : null)
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

  public ForelderDto toDto(Forelder forelder) {
    var foedselsdato = personopplysningService.henteFoedselsdato(forelder.getFoedselsnummer());
    var navnDto = modelMapper(personopplysningService.henteNavn(forelder.getFoedselsnummer()), NavnDto.class);
    var forelderDto = modelMapper.map(forelder, ForelderDto.class);
    forelderDto.setFoedselsdato(foedselsdato);
    forelderDto.setNavn(navnDto);
    return forelderDto;
  }

  public Farskapserklaering toEntity(FarskapserklaeringDto farskapserklaeringDto) {

    var dokument = toEntity(farskapserklaeringDto.getDokument());
    var farskapserklaering = modelMapper.map(farskapserklaeringDto, Farskapserklaering.class);
    farskapserklaering.setDokument(dokument);
    return farskapserklaering;
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
    return StatusKontrollereFar.builder().mor(mor).tidspunktForNullstilling(statusKontrollereFarDto.getTidspunktForNullstilling())
        .antallFeiledeForsoek(statusKontrollereFarDto.getAntallFeiledeForsoek()).build();
  }

  public StatusKontrollereFarDto toDto(StatusKontrollereFar statusKontrollereFar) {
    var morDto = toDto(statusKontrollereFar.getMor());
    return StatusKontrollereFarDto.builder().mor(morDto).tidspunktForNullstilling(statusKontrollereFar.getTidspunktForNullstilling())
        .antallFeiledeForsoek(statusKontrollereFar.getAntallFeiledeForsoek()).build();
  }
}