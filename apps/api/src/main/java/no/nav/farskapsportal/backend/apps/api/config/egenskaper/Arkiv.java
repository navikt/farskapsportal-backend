package no.nav.farskapsportal.backend.apps.api.config.egenskaper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Arkiv {
  private int arkiveringsintervall;
  private int maksAntallFeilPaaRad;
  private String arkiveringsforsinkelse;
  private String deaktiveringsrate;
  private String dokumentmigreringsrate = "0 30 4 * * ?";
  private String dokumentslettingsrate = "0 30 2 * * ?";
  private int levetidIkkeFerdigstilteSigneringsoppdragIDager;
  private int levetidOversendteFarskapserklaeringerIDager;
  private int levetidErklaeringerIkkeSignertAvMorIDager;
  private int levetidDokumenterIMaaneder = 12;
  private int maksAntallDokumenterSomSlettesPerKjoering = 1000;
}
