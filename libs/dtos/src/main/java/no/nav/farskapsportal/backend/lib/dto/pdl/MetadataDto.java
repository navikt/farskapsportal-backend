package no.nav.farskapsportal.backend.lib.dto.pdl;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class MetadataDto {

  String opplysningsId;
  @NotEmpty String master;
  @NotNull List<EndringDto> endringer;
  Boolean historisk;
}
