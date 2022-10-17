package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.dto.pdl.DoedsfallDto;

@Value
@Getter
public class HentPersonDoedsfall implements HentPersonSubResponse {

  String response;

  public HentPersonDoedsfall(DoedsfallDto doedsfallDto) {
    this.response = buildResponse(doedsfallDto);
  }

  private String buildResponse(DoedsfallDto doedsfallDto) {

    if (doedsfallDto != null && doedsfallDto.getDoedsdato() != null) {

      return String.join(
          "\n",
          " \"doedsfall\": [",
          " {",
          " \"doedsdato\": \"" + doedsfallDto.getDoedsdato() + "\",",
          " \"metadata\": {",
          " \"opplysningsId\": \" 123 \",",
          " \"master\": \"FREG\"",
          " }",
          " }",
          "]");
    } else {
      return String.join("\n", " \"doedsfall\": [", "]");
    }
  }

  @Override
  public String hentRespons(boolean medHistorikk) {
    return medHistorikk ? response : response;
  }
}
