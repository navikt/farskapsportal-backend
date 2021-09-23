package no.nav.farskapsportal.backend.lib.dto.pdl;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.backend.lib.dto.pdl.ForelderBarnRelasjonRolle.Sivilstandtype;

@Value
@Builder
public class SivilstandDto implements PdlDto {

  Sivilstandtype type;
  LocalDate gyldigFraOgMed;
  String relatertVedSivilstand;
  String bekreftelsesdato;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
