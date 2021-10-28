package no.nav.farskapsportal.backend.libs.dto.joark.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Sak {

  private Sakstype sakstype;

  private String fagsakId;

  private Fagsaksystem fagsaksystem;
}
