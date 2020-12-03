package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BarnDto {
  @ApiModelProperty(value = "Barnets termindato", example = "2021-05-02")
  private LocalDate termindato;

  @ApiModelProperty(value = "Barnets fødselsnummer hvis tilgjengelig", example = "01010112345")
  private String foedselsnummer;

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("Barn knyttet til termindato: ").append(termindato);
    return builder.toString();
  }
}
