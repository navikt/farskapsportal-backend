package no.nav.farskapsportal.backend.lib.felles.test.stub.consumer.pdl.stub;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.backend.lib.dto.KodeLand;

@Value
@Getter
public class HentPersonFoedsel implements HentPersonSubResponse {

  String response;

  public HentPersonFoedsel(LocalDate foedselsdato, boolean historisk) {
    this.response = buildResponse(foedselsdato, KodeLand.NORGE.getKodeLand(), "ASKIM", "123", historisk);
  }

  public HentPersonFoedsel(LocalDate foedselsdato, String foedested, boolean historisk) {
    this.response = buildResponse(foedselsdato, KodeLand.NORGE.getKodeLand(), foedested, "123", historisk);
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

  @Override
  public String hentRespons(boolean medHistorikk) {
    return medHistorikk ? response : response;
  }
}
