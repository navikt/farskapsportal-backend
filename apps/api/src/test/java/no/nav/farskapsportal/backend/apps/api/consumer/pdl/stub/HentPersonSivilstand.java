package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.dto.pdl.ForelderBarnRelasjonRolle.Sivilstandtype;

@Value
@Getter
public class HentPersonSivilstand implements HentPersonSubResponse {

  String response;

  public HentPersonSivilstand(Sivilstandtype sivilstandtype) {
    this.response = buildResponseSivilstand(sivilstandtype);
    var t = true;
  }

  private String buildResponseSivilstand(Sivilstandtype sivilstandtype) {
    if (sivilstandtype == null) {
      return String.join("\n", " \"sivilstand\": [", "]");
    } else {
      var startingElements = String.join("\n", " \"sivilstand\": [");
      var closingElements = String.join("\n", "]");

      return startingElements + hentSivilstandElement(sivilstandtype, LocalDateTime.now(), "123") + closingElements;
    }
  }

  private String hentSivilstandElement(Sivilstandtype sivilstandtype, LocalDateTime tidspunktOpprettet, String opplysningsId) {
    var sivilstandElement = new StringBuilder();

    sivilstandElement.append(String.join("\n", " {", " \"type\": \"" + sivilstandtype + "\","));
    if (tidspunktOpprettet != null) {
      sivilstandElement.append(PdlApiStub.hentFolkerigstermetadataElement(tidspunktOpprettet));
      sivilstandElement.append(",");
    }
    sivilstandElement.append(PdlApiStub.hentMetadataElement(opplysningsId, false, tidspunktOpprettet));
    sivilstandElement.append(String.join("\n", " }"));

    return sivilstandElement.toString();
  }

  @Override
  public String hentRespons(boolean medHistorikk) {
    return medHistorikk ? response : response;
  }
}
