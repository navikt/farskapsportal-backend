package no.nav.farskapsportal.backend.libs.dto.pdl;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MetadataDto {

  String opplysningsId;
  @NotEmpty String master;
  @NotNull List<EndringDto> endringer;
  Boolean historisk;
}
