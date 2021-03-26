package no.nav.farskapsportal.util;

import java.net.URI;
import java.net.URISyntaxException;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.dto.StatusKontrollereFarDto;
import no.nav.farskapsportal.exception.MappingException;
import no.nav.farskapsportal.persistence.entity.Barn;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.persistence.entity.Signeringsinformasjon;
import no.nav.farskapsportal.persistence.entity.StatusKontrollereFar;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MappingUtil {

  @Autowired
  private ModelMapper modelMapper;

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

    return Dokument.builder().dokumentinnhold(Dokumentinnhold.builder()
        .innhold(dokumentDto.getInnhold()).build())
        .signeringsinformasjonMor(signeringsinformasjonMor)
        .signeringsinformasjonFar(signeringsinformasjonFar)
        .dokumentnavn(dokumentDto.getDokumentnavn())
        .build();
  }

  public DokumentDto toDto(Dokument dokument) {

    try {
      return DokumentDto.builder()
          .dokumentnavn(dokument.getDokumentnavn())
          .innhold(dokument.getDokumentinnhold().getInnhold())
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

  public ForelderDto toDto(Forelder forelder) {
    return modelMapper.map(forelder, ForelderDto.class);
  }

  public Farskapserklaering toEntity(FarskapserklaeringDto farskapserklaeringDto) {

    var dokument = toEntity(farskapserklaeringDto.getDokument());
    var farskapserklaering = modelMapper.map(farskapserklaeringDto, Farskapserklaering.class);
    farskapserklaering.setDokument(dokument);
    return farskapserklaering;
  }

  public FarskapserklaeringDto toDto(Farskapserklaering farskapserklaering) {
    var dokumentDto = toDto(farskapserklaering.getDokument());
    var farskapserklaeringDto = modelMapper.map(farskapserklaering, FarskapserklaeringDto.class);
    farskapserklaeringDto.setDokument(dokumentDto);

    return farskapserklaeringDto;
  }

  public StatusKontrollereFar toEntity(StatusKontrollereFarDto statusKontrollereFarDto) {
    var mor = toEntity(statusKontrollereFarDto.getMor());
    return StatusKontrollereFar.builder().mor(mor).tidspunktSisteFeiledeForsoek(statusKontrollereFarDto.getTidspunktSisteFeiledeForsoek())
        .antallFeiledeForsoek(statusKontrollereFarDto.getAntallFeiledeForsoek()).build();
  }

  public StatusKontrollereFarDto toDto(StatusKontrollereFar statusKontrollereFar) {
    var morDto = toDto(statusKontrollereFar.getMor());
    return StatusKontrollereFarDto.builder().mor(morDto).tidspunktSisteFeiledeForsoek(statusKontrollereFar.getTidspunktSisteFeiledeForsoek())
        .antallFeiledeForsoek(statusKontrollereFar.getAntallFeiledeForsoek()).build();
  }
}
