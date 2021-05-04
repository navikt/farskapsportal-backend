package no.nav.farskapsportal.consumer.pdl.stub;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class HentPersonFoedsel implements HentPersonSubResponse {
  String response;

  public HentPersonFoedsel(LocalDate foedselsdato, boolean historisk) {
    this.response = buildResponse(foedselsdato, "ASKIM", "123", historisk);
  }

  public HentPersonFoedsel(LocalDate foedselsdato, String foedested, boolean historisk) {
    this.response = buildResponse(foedselsdato, foedested, "123", historisk);
  }

  private String buildResponse(LocalDate foedselsdato, String foedested, String opplysningsId, boolean historisk) {
    if (foedselsdato == null) {
      return String.join("\n", " \"foedsel\": [", "]");
    } else {
      var fd = foedselsdato.toString();

      return String.join(
          "\n",
          " \"foedsel\": [",
          " {",
          " \"foedselsdato\": \"" + fd + "\",",
          " \"foedested\": \"" + foedested + "\",",
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
