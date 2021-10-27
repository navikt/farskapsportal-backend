package no.nav.farskapsportal.backend.libs.dto.joark.api;

public enum JournalStatus {
  INNGAAENDE_UTEN_DOKUMENTVARIANTER(JournalpostType.INNGAAENDE, false),
  INNGAAENDE_MED_DOKUMENTVARIANTER(JournalpostType.INNGAAENDE, true),
  UTGAAENDE_UTEN_DOKUMENTVARIANTER(JournalpostType.UTGAAENDE, false),
  UGAAENDE_MED_DOKUMENTVARIANTER(JournalpostType.UTGAAENDE, true),
  NOTAT_UTEN_DOKUMENTVARIANTER(JournalpostType.NOTAT, false),
  NOTAT_MED_DOKUMENTVARIANTER(JournalpostType.NOTAT, true);

  private String kode;

  JournalStatus(JournalpostType jpType, boolean medDokumentvarianter) {
    if (jpType.equals(JournalpostType.INNGAAENDE) && !medDokumentvarianter) {
      this.kode = "OD";
    } else if (jpType.equals(JournalpostType.INNGAAENDE) && medDokumentvarianter) {
      this.kode = "M";
    } else if (jpType.equals(JournalpostType.UTGAAENDE) && !medDokumentvarianter) {
      this.kode = "R";
    } else if (jpType.equals(JournalpostType.UTGAAENDE) && medDokumentvarianter) {
      this.kode = "D";
    } else if (jpType.equals(JournalpostType.NOTAT) && !medDokumentvarianter) {
      this.kode = "R";
    } else if (jpType.equals(JournalpostType.NOTAT) && medDokumentvarianter) {
      this.kode = "D";
    }
  }

  public String getKode() {
    return this.kode;
  }
}
