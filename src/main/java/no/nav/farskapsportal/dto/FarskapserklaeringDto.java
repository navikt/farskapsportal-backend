package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
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
public class FarskapserklaeringDto {
  @ApiModelProperty("Barnet farskapserklæringen gjelder")
  private BarnDto barn;
  @ApiModelProperty("Barnets mor")
  private ForelderDto mor;
  @ApiModelProperty("Barnets far")
  private ForelderDto far;
  @ApiModelProperty("Farskapserklæringsdokumentet")
  private DokumentDto dokument;
}
