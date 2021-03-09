package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Builder
@Getter
@Setter
public class StatusKontrollereFarDto {

  @ApiModelProperty(value = "Barnets mor")
  private final ForelderDto mor;

  @ApiModelProperty(value = "Mors antall forsøk på å finne frem til riktig kombinasjon av fars navn og fødselsnummer")
  private final int antallFeiledeForsoek;

  @ApiModelProperty(value = "Tidspunkt for siste feilede forsøk")
  private final LocalDateTime tidspunktSisteFeiledeForsoek;

  @ApiModelProperty(value = "Mors resterende antall forsoek på å finne frem til riktig kombinasjon av fars navn og fødselsnummer")
  private int antallResterendeForsoek;

}
