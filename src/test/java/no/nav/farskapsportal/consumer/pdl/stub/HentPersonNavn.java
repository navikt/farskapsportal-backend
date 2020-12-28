package no.nav.farskapsportal.consumer.pdl.stub;

import lombok.Getter;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;

@Getter
public class HentPersonNavn implements HentPersonSubQuery {

  private String query;

  public HentPersonNavn(NavnDto navn) {
    buildQuery(navn, "sagga-dagga");
  }

  private void buildQuery(NavnDto navnDto, String opplysningsId) {
    if (navnDto == null) {
      this.query = String.join("\n", " \"navn\": [", "]");
    } else {

      var fornavn = navnDto.getFornavn() == null ? null : "\"" + navnDto.getFornavn() + "\"";
      var mellomnavn =
          navnDto.getMellomnavn() == null ? null : "\"" + navnDto.getMellomnavn() + "\"";
      var etternavn = navnDto.getEtternavn() == null ? null : "\"" + navnDto.getEtternavn() + "\"";

      this.query =
          String.join(
              "\n",
              " \"navn\": [",
              " {",
              " \"fornavn\": " + fornavn + ",",
              " \"mellomnavn\":" + mellomnavn + ",",
              " \"etternavn\":" + etternavn + ",",
              " \"metadata\": {",
              " \"opplysningsId\": \"" + opplysningsId + "\",",
              " \"master\": \"Freg\"",
              " }",
              " }",
              "]");
    }
  }
}
