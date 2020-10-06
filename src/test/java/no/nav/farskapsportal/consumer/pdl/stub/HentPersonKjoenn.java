package no.nav.farskapsportal.consumer.pdl.stub;

import lombok.Getter;
import no.nav.farskapsportal.api.Kjoenn;

@Getter
public class HentPersonKjoenn implements HentPersonSubQuery {

  private String query;

  public HentPersonKjoenn() {
    buildQuery(Kjoenn.KVINNE, "szagg, aog");
  }

  public HentPersonKjoenn(Kjoenn kjoenn) {
    buildQuery(kjoenn, "sagga-dagga");
  }

  private void buildQuery(Kjoenn kjoenn, String opplysningsId) {
    if (kjoenn == null) {
      this.query = String.join("\n", " \"kjoenn\": [", "]");
    } else {
      this.query =
          String.join(
              "\n",
              " \"kjoenn\": [",
              " {",
              " \"kjoenn\": \"" + kjoenn + "\",",
              " \"metadata\": {",
              " \"opplysningsId\": \"" + opplysningsId + "\",",
              " \"master\": \"Freg\"",
              " }",
              " }",
              "]");
    }
  }
}
