package no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub;

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

  public HentPersonKjoenn(LinkedHashMap<KjoennType, LocalDateTime> kjoennshistorikk) {
    this.responsMedHistorikk = buildResponseKjoenn(kjoennshistorikk, true);
    this.responsUtenHistorikk = buildResponseKjoenn(kjoennshistorikk, false);
  }

  private String buildResponseKjoenn(LinkedHashMap<KjoennType, LocalDateTime> input, boolean medHistorikk) {
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
        if (!medHistorikk) {
          if (!historisk) {
            kjoennshistorikk.append(hentKjoennElement(kjoenn.getKey(), kjoenn.getValue(), "123", false));
            kjoennshistorikk.append(closingElements);
            return kjoennshistorikk.toString();
          }
        } else {
          kjoennshistorikk.append(hentKjoennElement(kjoenn.getKey(), kjoenn.getValue(), "123", historisk));
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
      KjoennType kjoenn, LocalDateTime gyldighetstidspunkt, String opplysningsId, boolean historisk) {

    var kjoennElement = new StringBuilder();

    kjoennElement.append(String.join("\n", " {", " \"kjoenn\": \"" + kjoenn + "\","));
    if (gyldighetstidspunkt != null) {
      kjoennElement.append(PdlApiStub.hentFolkerigstermetadataElement(gyldighetstidspunkt));
      kjoennElement.append(",");
    }
    kjoennElement.append(PdlApiStub.hentMetadataElement(opplysningsId, historisk));
    kjoennElement.append(String.join("\n", " }"));

    return kjoennElement.toString();
  }

  @Override
  public String hentRespons(boolean medHistorikk) {
    return medHistorikk ? responsMedHistorikk : responsUtenHistorikk;
  }
}
