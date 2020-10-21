package no.nav.farskapsportal.dto;

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
  private Forelderrolle forelderRolle;
  private @NonNull String foedselsnummer;
  private @NonNull String fornavn;
  private String mellomnavn;
  private @NonNull String etternavn;
}
