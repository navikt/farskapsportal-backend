package no.nav.farskapsportal.consumer.pdl.stub;

import lombok.Getter;
import no.nav.farskapsportal.consumer.pdl.api.NavnDto;

@Getter
public class HentPersonNavn implements HentPersonSubResponse {

  private String response;

  public HentPersonNavn(NavnDto navn) {
    buildResponse(navn, "sagga-dagga");
  }

  private void buildResponse(NavnDto navnDto, String opplysningsId) {
    if (navnDto == null) {
      this.response = String.join("\n", " \"navn\": [", "]");
    } else {

      var fornavn = navnDto.getFornavn() == null ? null : "\"" + navnDto.getFornavn() + "\"";
      var mellomnavn =
          navnDto.getMellomnavn() == null ? null : "\"" + navnDto.getMellomnavn() + "\"";
      var etternavn = navnDto.getEtternavn() == null ? null : "\"" + navnDto.getEtternavn() + "\"";

      this.response =
          String.join(
              "\n",
              " \"navn\": [",
              " {",
              " \"fornavn\": " + fornavn + ",",
              " \"mellomnavn\":" + mellomnavn + ",",
              " \"etternavn\":" + etternavn + ",",
              " \"metadata\": {",
              " \"opplysningsId\": \"" + opplysningsId + "\",",
              " \"master\": \"Freg\"",
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
