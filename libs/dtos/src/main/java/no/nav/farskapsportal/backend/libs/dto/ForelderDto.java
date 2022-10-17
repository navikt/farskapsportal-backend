package no.nav.farskapsportal.backend.libs.dto;

import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForelderDto {

  @Parameter(description = "Forelderens rolle", example = "FAR")
  private Forelderrolle forelderrolle;

  @Parameter(description = "Fødselsdato", example = "01.01.1990")
  private LocalDate foedselsdato;

  @Parameter(description = "Forelderens fødselsnummer", example = "11111122222")
  private @NonNull String foedselsnummer;

  @Parameter(description = "Forelderens navn")
  private @NonNull NavnDto navn;

}
