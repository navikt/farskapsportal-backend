package no.nav.farskapsportal.backend.libs.dto.pdl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Personident {

  private String ident;
  private Identgruppe gruppe;
  private boolean historisk;

  public enum Identgruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID
  }
}