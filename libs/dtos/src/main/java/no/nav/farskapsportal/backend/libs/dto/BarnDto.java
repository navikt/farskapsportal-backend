package no.nav.farskapsportal.backend.libs.dto;

import io.swagger.v3.oas.annotations.Parameter;
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

  @Parameter(description = "Barnets termindato", example = "2021-05-02")
  private LocalDate termindato;

  @Parameter(description = "Barnets fødselsdato", example = "2021-08-01")
  LocalDate foedselsdato;

  @Parameter(description = "Barnets fødselsnummer hvis tilgjengelig")
  private String foedselsnummer;

  @Parameter(description = "Barnets fødested")
  private String foedested;

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    if (null != foedselsnummer) {
      builder.append("Barn med fødselsnummer: ").append(foedselsnummer);
    } else {
      builder.append("Barn knyttet til termindato: ").append(termindato);
    }
    return builder.toString();
  }
}
