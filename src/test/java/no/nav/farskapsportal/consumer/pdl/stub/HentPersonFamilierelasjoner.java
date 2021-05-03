package no.nav.farskapsportal.consumer.pdl.stub;

import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.consumer.pdl.api.FamilierelasjonerDto;

@Value
@Getter
public class HentPersonFamilierelasjoner implements HentPersonSubResponse {

  String response;

  public HentPersonFamilierelasjoner(
      FamilierelasjonerDto familierelasjonerDto, String opplysningsId) {
    this.response = buildResponse(familierelasjonerDto, opplysningsId);
  }

  private String buildResponse(FamilierelasjonerDto familierelasjonerDto, String opplysningsId) {
    if (familierelasjonerDto == null) {
      return String.join("\n", " \"familierelasjoner\": [", "]");
    } else {
      return String.join(
          "\n",
          " \"familierelasjoner\": [",
          " {",
          " \"relatertPersonsIdent\": \"" + familierelasjonerDto.getRelatertPersonsIdent() + "\",",
          " \"relatertPersonsRolle\": \"" + familierelasjonerDto.getRelatertPersonsRolle() + "\",",
          " \"minRolleForPerson\": \"" + familierelasjonerDto.getMinRolleForPerson() + "\",",
          " \"metadata\": {",
          " \"opplysningsId\": \"" + opplysningsId + "\",",
          " \"master\": \"FREG\"",
          " }",
          " }",
          "]");
    }
  }
}
