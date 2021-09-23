package no.nav.farskapsportal.backend.lib.dto;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NavnDto {

  @Parameter(description = "Personens fornavn", example = "Karl Donald")
  private String fornavn;

  @Parameter(description = "Personens mellomnavn", example = "Jensen")
  private String mellomnavn;

  @Parameter(description = "Personens etternavn", example = "Duck")
  private String etternavn;

  public String sammensattNavn() {
    return Stream.of(getFornavn(), getMellomnavn(), getEtternavn())
        .filter(s -> s != null && !toString().isEmpty()).collect(
            Collectors.joining(" "));
  }
}
