package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
