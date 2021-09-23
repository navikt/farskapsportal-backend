package no.nav.farskapsportal.backend.asynkron.consumer.joark.api;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DokumentVariant {

  @NotNull(message = "Filtype kan ikke være null")
  private String filtype;

  @NotNull(message = "Variantformat kan ikke være null")
  private String variantformat;

  private byte[] fysiskDokument;

  private String filnavn;
}

