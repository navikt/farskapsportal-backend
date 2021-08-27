package no.nav.farskapsportal.consumer.pdl.api;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NavnDto implements PdlDto {

  String fornavn;
  String mellomnavn;
  String etternavn;
  String forkortetNavn;
  PersonnavnDto originaltNavn;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;

  public String sammensattNavn() {
    return Stream.of(getFornavn(), getMellomnavn(), getEtternavn())
        .filter(s -> s != null && !toString().isEmpty()).collect(
            Collectors.joining(" "));
  }
}
