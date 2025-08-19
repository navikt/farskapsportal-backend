package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import java.time.LocalDate;
import java.time.Year;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class HentPersonFødselsdato implements HentPersonSubResponse {

  String response;

  public HentPersonFødselsdato(LocalDate fødselsdato, Year fødselsår, boolean historisk) {
    this.response = buildResponse(fødselsdato, fødselsår, "123", historisk);
  }

  public HentPersonFødselsdato(LocalDate fødselsdato, boolean historisk) {
    this.response = buildResponse(fødselsdato, Year.of(fødselsdato.getYear()), "123", historisk);
  }

  private String buildResponse(
      LocalDate fødselsdato, Year fødselsår, String opplysningsId, boolean historisk) {
    if (fødselsdato == null) {
      return String.join("\n", " \"fødselsdato\": [", "]");
    } else {
      var fd = fødselsdato.toString();
      var få = fødselsår.toString();

      return String.join(
          "\n",
          " \"fødselsdato\": [",
          " {",
          " \"fødselsdato\": \"" + fd + "\",",
          " \"fødselsår\": \"" + få + "\",",
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
