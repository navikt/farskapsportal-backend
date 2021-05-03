package no.nav.farskapsportal.consumer.pdl.stub;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class HentPersonFoedsel implements HentPersonSubResponse {
  String response;

  public HentPersonFoedsel(LocalDate foedselsdato, boolean historisk) {
    this.response = buildResponse(foedselsdato, "123", historisk);
  }

  private String buildResponse(LocalDate foedselsdato, String opplysningsId, boolean historisk) {
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
