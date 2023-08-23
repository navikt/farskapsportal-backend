package no.nav.farskapsportal.backend.apps.api.consumer.pdl.stub;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.backend.libs.dto.pdl.KjoennType;

@Value
@Getter
public class HentPersonKjoenn implements HentPersonSubResponse {

  String responsMedHistorikk;

  String responsUtenHistorikk;

  public HentPersonKjoenn(LinkedHashMap<LocalDateTime, KjoennType> kjoennshistorikk) {
    this.responsMedHistorikk = buildResponseKjoenn(kjoennshistorikk, true);
    this.responsUtenHistorikk = buildResponseKjoenn(kjoennshistorikk, false);
    var t = false;
  }

  private String buildResponseKjoenn(
      LinkedHashMap<LocalDateTime, KjoennType> input, boolean medHistorikk) {
    if (input == null || input.isEmpty()) {
      return String.join("\n", " \"kjoenn\": [", "]");
    } else {

      var startingElements = String.join("\n", " \"kjoenn\": [");
      var closingElements = String.join("\n", "]");

      var kjoennshistorikk = new StringBuilder();
      kjoennshistorikk.append(startingElements);

      var count = 0;
      for (Map.Entry<LocalDateTime, KjoennType> kjoenn : input.entrySet()) {
        var historisk = input.size() > 1 && (count == 0 || count < input.size() - 2);
        if (!medHistorikk) {
          if (!historisk) {
            kjoennshistorikk.append(
                hentKjoennElement(kjoenn.getValue(), kjoenn.getKey(), "123", false));
            kjoennshistorikk.append(closingElements);
            return kjoennshistorikk.toString();
          }
        } else {
          kjoennshistorikk.append(
              hentKjoennElement(kjoenn.getValue(), kjoenn.getKey(), "123", historisk));
          if (input.size() > 1 && (count == 0 || count > input.size() - 1)) {
            kjoennshistorikk.append(",");
          }
        }
        count++;
      }

      kjoennshistorikk.append(closingElements);
      return kjoennshistorikk.toString();
    }
  }

  private String hentKjoennElement(
      KjoennType kjoenn, LocalDateTime registrert, String opplysningsId, boolean historisk) {

    var kjoennElement = new StringBuilder();

    kjoennElement.append(String.join("\n", " {", " \"kjoenn\": \"" + kjoenn + "\","));
    kjoennElement.append(PdlApiStub.hentMetadataElement(opplysningsId, historisk, registrert));
    kjoennElement.append(String.join("\n", " }"));

    return kjoennElement.toString();
  }

  @Override
  public String hentRespons(boolean medHistorikk) {
    return medHistorikk ? responsMedHistorikk : responsUtenHistorikk;
  }
}
