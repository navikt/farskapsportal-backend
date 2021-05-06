package no.nav.farskapsportal.consumer.pdl.stub;

import static no.nav.farskapsportal.service.FarskapsportalService.KODE_LAND_NORGE;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class HentPersonFoedsel implements HentPersonSubResponse {

  String response;

  public HentPersonFoedsel(LocalDate foedselsdato, boolean historisk) {
    this.response = buildResponse(foedselsdato, KODE_LAND_NORGE, "ASKIM", "123", historisk);
  }

  public HentPersonFoedsel(LocalDate foedselsdato, String foedested, boolean historisk) {
    this.response = buildResponse(foedselsdato, KODE_LAND_NORGE, foedested, "123", historisk);
  }

  public HentPersonFoedsel(LocalDate foedselsdato, String foedeland, String foedested, boolean historisk) {
    this.response = buildResponse(foedselsdato, foedeland, foedested, "123", historisk);
  }

  private String buildResponse(LocalDate foedselsdato, String foedeland, String foedested, String opplysningsId, boolean historisk) {
    if (foedselsdato == null) {
      return String.join("\n", " \"foedsel\": [", "]");
    } else {
      var fd = foedselsdato.toString();

      return String.join(
          "\n",
          " \"foedsel\": [",
          " {",
          " \"foedselsdato\": \"" + fd + "\",",
          " \"foedeland\": \"" + foedeland + "\",",
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
