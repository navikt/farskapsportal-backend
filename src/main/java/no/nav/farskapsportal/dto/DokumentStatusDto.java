package no.nav.farskapsportal.dto;

import java.net.URI;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
@Getter
public class DokumentStatusDto {

  boolean erSigneringsjobbenFerdig;
  List<SignaturDto> signaturer;
  URI padeslenke;
  URI statuslenke;
}
