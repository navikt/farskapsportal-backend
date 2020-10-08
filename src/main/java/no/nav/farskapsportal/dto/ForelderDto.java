package no.nav.farskapsportal.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import no.nav.farskapsportal.api.ForelderRolle;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
@Getter
@Setter
public class ForelderDto {
  private ForelderRolle forelderRolle = ForelderRolle.FAR;
  private @NonNull String foedselsnummer;
  private @NonNull String fornavn;
  private String mellomnavn;
  private @NonNull String etternavn;
}
