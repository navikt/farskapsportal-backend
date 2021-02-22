package no.nav.farskapsportal.util;

import java.net.URI;
import java.net.URISyntaxException;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.DokumentDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.MappingException;
import no.nav.farskapsportal.persistence.entity.Barn;
import no.nav.farskapsportal.persistence.entity.Dokument;
import no.nav.farskapsportal.persistence.entity.Farskapserklaering;
import no.nav.farskapsportal.persistence.entity.Forelder;
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
    return modelMapper.map(dokumentDto, Dokument.class);
  }

  public DokumentDto toDto(Dokument dokument) {

    try {
      var dto = modelMapper.map(dokument, DokumentDto.class);

      dto.setPadesUrl(dokument.getPadesUrl() != null ? new URI(dokument.getPadesUrl()): null);
      dto.setDokumentStatusUrl(dokument.getDokumentStatusUrl() != null ? new URI(dokument.getDokumentStatusUrl()) : null);
      dto.setRedirectUrlFar(dokument.getRedirectUrlFar() != null ? new URI(dokument.getRedirectUrlFar()): null);
      dto.setRedirectUrlMor(dokument.getRedirectUrlMor() != null ? new URI(dokument.getRedirectUrlMor()) : null);

      return dto;
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
}
