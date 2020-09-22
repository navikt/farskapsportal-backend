package no.nav.farskapsportal.consumer.pdl.api;

import lombok.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetadataDto {
    private String opplysningsId;
    @NotEmpty
    private String master;
    @NotNull
    private List<EndringDto> endringer;
    @Setter
    private Boolean historisk;
}
