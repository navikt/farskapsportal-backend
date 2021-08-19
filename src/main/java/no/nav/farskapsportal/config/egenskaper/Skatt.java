package no.nav.farskapsportal.config.egenskaper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Skatt {

  private int maksAntallForbindelser;
  private int maksAntallForbindelserPerRute;
  private int maksVentetidLesing;
  private int maksVentetidForbindelse;
}
