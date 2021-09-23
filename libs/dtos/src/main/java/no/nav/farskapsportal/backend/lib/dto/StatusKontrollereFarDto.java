package no.nav.farskapsportal.backend.lib.dto;

import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Builder
@Getter
@Setter
public class StatusKontrollereFarDto {

  @Parameter(description = "Barnets mor")
  private final ForelderDto mor;

  @Parameter(description = "Mors antall forsøk på å finne frem til riktig kombinasjon av fars navn og fødselsnummer")
  private final int antallFeiledeForsoek;

  @Parameter(description = "Tidspunkt for nullstilling av antall feilede forsøk")
  private final LocalDateTime tidspunktForNullstilling;

  @Parameter(description = "Mors resterende antall forsoek på å finne frem til riktig kombinasjon av fars navn og fødselsnummer")
  private int antallResterendeForsoek;

}
