package no.nav.farskapsportal.dto;

import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.farskapsportal.api.Rolle;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FarskapserklaeringDto {
  @ApiModelProperty("Unik id for farskapserklæring")
  private int idFarskapserklaering;
  @ApiModelProperty("Paalogget brukers rolle i farskapserklæringen")
  private Rolle paaloggetBrukersRolle;
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
  @ApiModelProperty("Unik ID for farskapserklæringen som brukes ved oversendelse til Skatt")
  private long meldingsidSkatt;
  @ApiModelProperty("Tidspunkt farskapserklæringen ble sendt til Skatt")
  private LocalDateTime sendtTilSkatt;
}
