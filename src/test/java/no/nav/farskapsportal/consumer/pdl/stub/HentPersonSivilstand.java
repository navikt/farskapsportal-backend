package no.nav.farskapsportal.consumer.pdl.stub;

import static no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub.hentFolkerigstermetadataElement;
import static no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub.hentMetadataElement;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.api.Sivilstandtype;

@Value
@Getter
public class HentPersonSivilstand implements HentPersonSubResponse {

  String response;

  public HentPersonSivilstand(Sivilstandtype sivilstandtype) {
    this.response = buildResponseSivilstand(sivilstandtype);
  }

  private String buildResponseSivilstand(Sivilstandtype sivilstandtype) {
    if (sivilstandtype == null) {
      return String.join("\n", " \"sivilstand\": [", "]");
    } else {
      var startingElements = String.join("\n", " \"sivilstand\": [");
      var closingElements = String.join("\n", "]");

      return startingElements + hentSivilstandElement(sivilstandtype, null, "123") + closingElements;
    }
  }

  private String hentSivilstandElement(Sivilstandtype sivilstandtype, LocalDateTime gyldighetstidspunkt, String opplysningsId) {
    var sivilstandElement = new StringBuilder();

    sivilstandElement.append(String.join("\n", " {", " \"type\": \"" + sivilstandtype + "\","));
    if (gyldighetstidspunkt != null) {
      sivilstandElement.append(hentFolkerigstermetadataElement(gyldighetstidspunkt));
      sivilstandElement.append(",");
    }
    sivilstandElement.append(hentMetadataElement(opplysningsId, false));
    sivilstandElement.append(String.join("\n", " }"));

    return sivilstandElement.toString();
  }
}
