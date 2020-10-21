package no.nav.farskapsportal.consumer.pdl.stub;

import java.util.List;
import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.api.Kjoenn;

@Value
@Getter
public class HentPersonKjoenn implements HentPersonSubQuery {

  String query;

  public HentPersonKjoenn(Kjoenn kjoenn) {
    this.query = buildQueryKjoenn(kjoenn, "sagga-dagga");
  }

  public HentPersonKjoenn(List<Kjoenn> kjoennshistorikk) {
    this.query = buildQueryKjoennMedHistorikk(kjoennshistorikk);
  }

  private String buildQueryKjoennMedHistorikk(List<Kjoenn> input) {
    if (input == null || input.isEmpty()) {
      return String.join("\n", " \"kjoenn\": [", "]");
    } else {

      var startingElements = String.join("\n", " \"kjoenn\": [");
      var closingElements = String.join("\n", "]");

      var kjoennshistorikk = new StringBuilder();
      kjoennshistorikk.append(startingElements);

      var count = 0;
      for (Kjoenn kjoenn : input) {
        kjoennshistorikk.append(hentKjoennElement(kjoenn, "123"));
        if (input.size() > 1 && (count == 0 || count > input.size() - 1)) {
          kjoennshistorikk.append(",");
        }
        count++;
      }

      kjoennshistorikk.append(closingElements);
      return kjoennshistorikk.toString();
    }
  }

  private String buildQueryKjoenn(Kjoenn kjoenn, String opplysningsId) {
    if (kjoenn == null) {
      return String.join("\n", " \"kjoenn\": [", "]");
    } else {
      var startingElements = String.join("\n", " \"kjoenn\": [");
      var closingElements = String.join("\n", "]");

      return startingElements + hentKjoennElement(kjoenn, opplysningsId) + closingElements;
    }
  }

  private String hentKjoennElement(Kjoenn kjoenn, String opplysningsId) {
    return String.join(
        "\n",
        " {",
        " \"kjoenn\": \"" + kjoenn + "\",",
        " \"metadata\": {",
        " \"opplysningsId\": \"" + opplysningsId + "\",",
        " \"master\": \"Freg\"",
        " }",
        " }");
  }
}
