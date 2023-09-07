package no.nav.farskapsportal.backend.libs.dto.pdl;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HentIdenter {
  private List<Personident> identer = new ArrayList<>();
}
