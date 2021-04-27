package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import no.nav.farskapsportal.api.Forelderrolle;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForelderDto {

  @ApiModelProperty(value = "Forelderens rolle", example = "MOR")
  private Forelderrolle forelderrolle;

  @ApiModelProperty(value = "Fødselsdato", example="01.01.1990")
  private LocalDate foedselsdato;

  @ApiModelProperty(value = "Forelderens fødselsnummer", example = "12345678910")
  private @NonNull String foedselsnummer;

  @ApiModelProperty(value = "Forelderens fornavn", example = "Kjøttdeig")
  private @NonNull String fornavn;

  @ApiModelProperty(value = "Forelderens mellomnavn", example = "")
  private String mellomnavn;

  @ApiModelProperty(value = "Forelderens etternavn", example = "Hammer")
  private @NonNull String etternavn;

}
