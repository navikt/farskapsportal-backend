package no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Arkiv {
  private int arkiveringsintervall;
  private String arkiveringsforsinkelse;
  private String deaktiveringsrate;
  private int levetidIkkeFerdigstilteSigneringsoppdragIDager;
  private int levetidOversendteFarskapserklaeringerIDager;
  private int levetidErklaeringerIkkeSignertAvMorIDager;
}
