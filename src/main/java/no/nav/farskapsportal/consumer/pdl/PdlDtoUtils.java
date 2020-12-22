package no.nav.farskapsportal.consumer.pdl;

import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import no.nav.farskapsportal.consumer.pdl.api.PdlDto;

@UtilityClass
public class PdlDtoUtils {

  public static final String MASTER_PDL = "PDL";
  public static final String MASTER_FREG = "FREG";

  public static Predicate<PdlDto> isMasterPdlOrFreg() {
    return dto ->
        MASTER_PDL.equalsIgnoreCase(dto.getMetadata().getMaster())
            || MASTER_FREG.equalsIgnoreCase(dto.getMetadata().getMaster());
  }

  public static boolean isMasterPdlOrFreg(PdlDto dto) {
    return MASTER_PDL.equalsIgnoreCase(dto.getMetadata().getMaster())
        || MASTER_FREG.equalsIgnoreCase(dto.getMetadata().getMaster());
  }
}
