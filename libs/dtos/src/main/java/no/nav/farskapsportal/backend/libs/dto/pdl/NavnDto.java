package no.nav.farskapsportal.backend.libs.dto.pdl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NavnDto implements PdlDto {

  private String fornavn;
  private String mellomnavn;
  private String etternavn;
  private String forkortetNavn;
  private PersonnavnDto originaltNavn;
  private FolkeregistermetadataDto folkeregistermetadata;
  private MetadataDto metadata;

}
