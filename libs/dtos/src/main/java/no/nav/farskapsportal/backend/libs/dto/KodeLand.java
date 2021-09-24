package no.nav.farskapsportal.backend.libs.dto;

public enum KodeLand {
  NORGE("NOR");
  String kodeLand;

  KodeLand(String kodeLand){
    this.kodeLand = kodeLand;
  }

  public String getKodeLand() {
    return this.kodeLand;
  }

}
