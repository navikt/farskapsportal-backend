package no.nav.farskapsportal.consumer.pdl.stub;

import static no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub.hentFolkerigstermetadataElement;
import static no.nav.farskapsportal.consumer.pdl.stub.PdlApiStub.hentMetadataElement;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.consumer.pdl.api.KjoennType;

@Value
@Getter
public class HentPersonKjoenn implements HentPersonSubQuery {

  String query;

  public HentPersonKjoenn(KjoennType kjoenn) {
    this.query = buildQueryKjoenn(kjoenn, "sagga-dagga", false);
  }

  public HentPersonKjoenn(Map<KjoennType, LocalDateTime> kjoennshistorikk) {
    this.query = buildQueryKjoennMedHistorikk(kjoennshistorikk);
  }

  private String buildQueryKjoennMedHistorikk(Map<KjoennType, LocalDateTime> input) {
    if (input == null || input.isEmpty()) {
      return String.join("\n", " \"kjoenn\": [", "]");
    } else {

      var startingElements = String.join("\n", " \"kjoenn\": [");
      var closingElements = String.join("\n", "]");

      var kjoennshistorikk = new StringBuilder();
      kjoennshistorikk.append(startingElements);

      var count = 0;
      for (Map.Entry<KjoennType, LocalDateTime> kjoenn : input.entrySet()) {
        var historisk = input.size() > 1 && (count == 0 || count < input.size() - 2);
        kjoennshistorikk.append(hentKjoennElement(kjoenn.getKey(), kjoenn.getValue(), "123", historisk));
        if (input.size() > 1 && (count == 0 || count > input.size() - 1)) {
          kjoennshistorikk.append(",");
        }
        count++;
      }

      kjoennshistorikk.append(closingElements);
      return kjoennshistorikk.toString();
    }
  }

  private String buildQueryKjoenn(KjoennType kjoenn, String opplysningsId, boolean historisk) {
    if (kjoenn == null) {
      return String.join("\n", " \"kjoenn\": [", "]");
    } else {
      var startingElements = String.join("\n", " \"kjoenn\": [");
      var closingElements = String.join("\n", "]");

      return startingElements + hentKjoennElement(kjoenn, null, opplysningsId, historisk) + closingElements;
    }
  }

  private String hentKjoennElement(
      KjoennType kjoenn, LocalDateTime gyldighetstidspunkt, String opplysningsId, boolean historisk) {

    var kjoennElement = new StringBuilder();

    kjoennElement.append(String.join("\n", " {", " \"kjoenn\": \"" + kjoenn + "\","));
    if (gyldighetstidspunkt != null) {
      kjoennElement.append(hentFolkerigstermetadataElement(gyldighetstidspunkt));
      kjoennElement.append(",");
    }
    kjoennElement.append(hentMetadataElement(opplysningsId, historisk));
    kjoennElement.append(String.join("\n", " }"));

    return kjoennElement.toString();
  }
}
