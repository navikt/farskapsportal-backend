package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import lombok.Getter;
import no.nav.farskapsportal.backend.libs.dto.pdl.NavnDto;

@Getter
public class HentPersonNavn implements HentPersonSubResponse {

  private String response;

  public HentPersonNavn(NavnDto navnDto) {
    buildResponse(navnDto, "sagga-dagga");
  }

  public HentPersonNavn(no.nav.farskapsportal.backend.libs.dto.NavnDto navnDto) {
    buildResponse(NavnDto.builder().fornavn(navnDto.getFornavn()).mellomnavn(navnDto.getMellomnavn()).etternavn(navnDto.getEtternavn()).build(),
        "sagga-dagga");
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
