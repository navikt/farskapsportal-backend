package no.nav.farskapsportal.consumer.esignering.api;

import java.net.URI;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.api.StatusSignering;

@Value
@Builder
@Getter
public class DokumentStatusDto {

  boolean erSigneringsjobbenFerdig;
  List<SignaturDto> signaturer;
  URI padeslenke;
  URI statuslenke;
  URI bekreftelseslenke;
  StatusSignering status;
}
