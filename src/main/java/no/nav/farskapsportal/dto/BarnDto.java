package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class BarnDto {
  @ApiModelProperty("Barnets termindato")
  LocalDate termindato;
  @ApiModelProperty("Barnets fødselsnummer hvis tilgjengelig")
  String foedselsnummer;

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Barn knyttet til termindato: ").append(termindato);
    return builder.toString();
  }
}
