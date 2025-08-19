package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class HentPersonFødested implements HentPersonSubResponse {

  String response;

  public HentPersonFødested(
      String fødeland, String fødested, String fødekommune, boolean historisk) {
    this.response = buildResponse(fødeland, fødested, fødekommune, "123", historisk);
  }

  public HentPersonFødested(String fødeland, String fødested, boolean historisk) {
    this.response = buildResponse(fødeland, fødested, "0123", "123", historisk);
  }

  public HentPersonFødested(String fødeland, boolean historisk) {
    this.response = buildResponse(fødeland, "ASKIM", "0123", "123", historisk);
  }

  private String buildResponse(
      String fødeland,
      String fødested,
      String fødekommune,
      String opplysningsId,
      boolean historisk) {
    if (fødeland == null) {
      return String.join("\n", " \"fødested\": [", "]");
    } else {
      return String.join(
          "\n",
          " \"fødested\": [",
          " {",
          " \"fødeland\": \"" + fødeland + "\",",
          " \"fødested\": \"" + fødested + "\",",
          " \"fødekommune\": \"" + fødekommune + "\",",
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
