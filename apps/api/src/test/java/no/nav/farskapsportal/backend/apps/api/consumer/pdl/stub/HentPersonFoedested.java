package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class HentPersonFoedested implements HentPersonSubResponse {

  String response;

  public HentPersonFoedested(
      String foedeland, String foedested, String foedekommune, boolean historisk) {
    this.response = buildResponse(foedeland, foedested, foedekommune, "123", historisk);
  }

  public HentPersonFoedested(String foedeland, String foedested, boolean historisk) {
    this.response = buildResponse(foedeland, foedested, "0123", "123", historisk);
  }

  public HentPersonFoedested(String foedeland, boolean historisk) {
    this.response = buildResponse(foedeland, "ASKIM", "0123", "123", historisk);
  }

  private String buildResponse(
      String foedeland,
      String foedested,
      String foedekommune,
      String opplysningsId,
      boolean historisk) {
    if (foedeland == null) {
      return String.join("\n", " \"foedested\": [", "]");
    } else {
      return String.join(
          "\n",
          " \"foedested\": [",
          " {",
          " \"foedeland\": \"" + foedeland + "\",",
          " \"foedested\": \"" + foedested + "\",",
          " \"foedekommune\": \"" + foedekommune + "\",",
          " \"metadata\": {",
          " \"opplysningsId\": \"" + opplysningsId + "\",",
          " \"master\": \"FREG\",",
          " \"historisk\": " + historisk,
          " }",
          " }",
          "]");
    }
  }

  @Override
  public String hentRespons(boolean medHistorikk) {
    return medHistorikk ? response : response;
  }
}
