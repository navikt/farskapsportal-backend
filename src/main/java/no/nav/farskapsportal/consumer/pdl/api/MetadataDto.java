package no.nav.farskapsportal.consumer.pdl.api;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetadataDto {
  private String opplysningsId;
  @NotEmpty private String master;
  @NotNull private List<EndringDto> endringer;
  @Setter private Boolean historisk;
}
