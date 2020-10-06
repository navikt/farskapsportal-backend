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

      var mellomnavn = navnDto.getMellomnavn() == null ? null : "\"" + navnDto.getMellomnavn() + "\"";

      if (navnDto.getMellomnavn() != null) {

      }
      this.query =
          String.join(
              "\n",
              " \"navn\": [",
              " {",
              " \"fornavn\": \"" + navnDto.getFornavn() + "\",",
              " \"mellomnavn\":"  + mellomnavn + ",",
              " \"etternavn\": \"" + navnDto.getEtternavn() + "\",",
              " \"metadata\": {",
              " \"opplysningsId\": \"" + opplysningsId + "\",",
              " \"master\": \"Freg\"",
              " }",
              " }",
              "]");
    }
  }
}
