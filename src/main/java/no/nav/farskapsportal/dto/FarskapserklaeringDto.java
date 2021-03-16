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
  @ApiModelProperty("Unik id for farskapserklæring")
  private int idFarskapserklaering;
  @ApiModelProperty("Barnet farskapserklæringen gjelder")
  private BarnDto barn;
  @ApiModelProperty("Barnets mor")
  private ForelderDto mor;
  @ApiModelProperty("Barnets far")
  private ForelderDto far;
  @ApiModelProperty("Mor oppgir om hun bor sammen med far")
  Boolean morBorSammenMedFar;
  @ApiModelProperty("Far oppgir om han bor sammen med mor")
  Boolean farBorSammenMedMor;
  @ApiModelProperty("Farskapserklæringsdokumentet")
  private DokumentDto dokument;
}
