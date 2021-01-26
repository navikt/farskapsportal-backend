package no.nav.farskapsportal.api;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FarskapserklaeringResponse {

  Optional<Feilkode> feilkode;

}
