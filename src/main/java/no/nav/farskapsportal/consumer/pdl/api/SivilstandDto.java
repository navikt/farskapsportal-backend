package no.nav.farskapsportal.consumer.pdl.api;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;
import no.nav.farskapsportal.api.Sivilstandtype;

@Value
@Builder
public class SivilstandDto implements PdlDto {

  Sivilstandtype type;
  LocalDate gyldigFraOgMed;
  String myndighet;
  String kommune;
  String sted;
  String utland;
  String relatertVedSivilstand;
  String bekreftelsesdato;
  FolkeregistermetadataDto folkeregistermetadata;
  MetadataDto metadata;
}
