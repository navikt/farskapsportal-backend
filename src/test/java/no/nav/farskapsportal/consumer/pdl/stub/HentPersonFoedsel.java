package no.nav.farskapsportal.consumer.pdl.stub;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class HentPersonFoedsel implements HentPersonSubQuery {
  String query;

  public HentPersonFoedsel(LocalDate foedselsdato, boolean historisk) {
    this.query = buildQuery(foedselsdato, "123", historisk);
  }

  private String buildQuery(LocalDate foedselsdato, String opplysningsId, boolean historisk) {
    if (foedselsdato == null) {
      return String.join("\n", " \"foedsel\": [", "]");
    } else {
      var fd = foedselsdato.toString();

      return String.join(
          "\n",
          " \"foedsel\": [",
          " {",
          " \"foedselsdato\": \"" + fd + "\",",
          " \"metadata\": {",
          " \"opplysningsId\": \"" + opplysningsId + "\",",
          " \"master\": \"FREG\",",
          " \"historisk\": " + historisk,
          " }",
          " }",
          "]");
    }
  }
}
