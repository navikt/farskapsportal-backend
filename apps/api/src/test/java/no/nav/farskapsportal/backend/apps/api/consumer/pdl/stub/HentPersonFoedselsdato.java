package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import java.time.LocalDate;
import java.time.Year;
import lombok.Getter;
import lombok.Value;

@Value
@Getter
public class HentPersonFoedselsdato implements HentPersonSubResponse {

  String response;

  public HentPersonFoedselsdato(LocalDate foedselsdato, Year foedselsår, boolean historisk) {
    this.response = buildResponse(foedselsdato, foedselsår, "123", historisk);
  }

  public HentPersonFoedselsdato(LocalDate foedselsdato, boolean historisk) {
    this.response = buildResponse(foedselsdato, Year.of(foedselsdato.getYear()), "123", historisk);
  }

  private String buildResponse(
      LocalDate foedselsdato, Year foedselsår, String opplysningsId, boolean historisk) {
    if (foedselsdato == null) {
      return String.join("\n", " \"foedselsdato\": [", "]");
    } else {
      var fd = foedselsdato.toString();
      var faa = foedselsår.toString();

      return String.join(
          "\n",
          " \"foedselsdato\": [",
          " {",
          " \"foedselsdato\": \"" + fd + "\",",
          " \"foedselsår\": \"" + faa + "\",",
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
