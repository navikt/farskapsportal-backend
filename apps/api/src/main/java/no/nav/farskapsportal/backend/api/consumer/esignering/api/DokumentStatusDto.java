package no.nav.farskapsportal.backend.api.consumer.esignering.api;

import java.net.URI;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.backend.api.api.StatusSignering;

@Value
@Builder
@Getter
public class DokumentStatusDto {

  List<SignaturDto> signaturer;
  URI padeslenke;
  URI statuslenke;
  URI bekreftelseslenke;
  StatusSignering statusSignering;
}
