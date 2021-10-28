package no.nav.farskapsportal.backend.libs.dto;

import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDateTime;
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
  @Parameter(description = "Unik id for farskapserklæring")
  private int idFarskapserklaering;
  @Parameter(description = "Paalogget brukers rolle i farskapserklæringen")
  private Rolle paaloggetBrukersRolle;
  @Parameter(description = "Barnet farskapserklæringen gjelder")
  private BarnDto barn;
  @Parameter(description = "Barnets mor")
  private ForelderDto mor;
  @Parameter(description = "Barnets far")
  private ForelderDto far;
  @Parameter(description = "Far oppgir om han bor sammen med mor")
  Boolean farBorSammenMedMor;
  @Parameter(description = "Farskapserklæringsdokumentet")
  private DokumentDto dokument;
  @Parameter(description = "Unik ID for farskapserklæringen som brukes ved oversendelse til Skatt")
  private String meldingsidSkatt;
  @Parameter(description = "Tidspunkt farskapserklæringen ble sendt til Skatt")
  private LocalDateTime sendtTilSkatt;
}
