package no.nav.farskapsportal.api;

import io.swagger.annotations.ApiModel;
import java.time.LocalDate;
import lombok.Value;

@ApiModel
@Value
public class BekrefteFarskapRequest {
  String farsNavn;
  String farsFodselsnummer;
  LocalDate termindato;
  String foedselsnummer;
}
