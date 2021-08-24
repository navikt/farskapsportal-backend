package no.nav.farskapsportal.consumer.pdl.stub;

import lombok.Getter;
import lombok.Value;
import no.nav.farskapsportal.consumer.pdl.api.VergemaalEllerFremtidsfullmaktDto;

@Value
@Getter
public class HentPersonVergeEllerFremtidsfullmakt implements HentPersonSubResponse {

  String response;

  public HentPersonVergeEllerFremtidsfullmakt(VergemaalEllerFremtidsfullmaktDto vergemaalEllerFremtidsfullmaktDto) {
    this.response = buildResponseVergeEllerFremtidsfullmakt(vergemaalEllerFremtidsfullmaktDto);
  }

  private String buildResponseVergeEllerFremtidsfullmakt(VergemaalEllerFremtidsfullmaktDto vergemaalEllerFremtidsfullmaktDto) {

    if (vergemaalEllerFremtidsfullmaktDto == null) {
      return String.join("\n", " \"vergemaalEllerFremtidsfullmakt\": [", "]");
    } else {
      var startingElements = String.join("\n", " \"vergemaalEllerFremtidsfullmakt\": [");
      var closingElements = String.join("\n", "]");

      return startingElements + henteVergemaalEllerFremtidsfullmakt(vergemaalEllerFremtidsfullmaktDto) + closingElements;
    }
  }

  private String henteVergemaalEllerFremtidsfullmakt(VergemaalEllerFremtidsfullmaktDto vergemaalEllerFremtidsfullmaktDto) {
    var vergemaalEllerFremtidsfullmaktElement = new StringBuilder();

    vergemaalEllerFremtidsfullmaktElement.append(String.join("\n", " {", " \"type\": \"" + vergemaalEllerFremtidsfullmaktDto.getType() + "\","));
    vergemaalEllerFremtidsfullmaktElement.append(String.join("\n", "  ", " \"embete\": \"" + vergemaalEllerFremtidsfullmaktDto.getEmbete() + "\","));
    vergemaalEllerFremtidsfullmaktElement.append(String.join("\n", "  ", " \"vergeEllerFullmektig\": {"));
    vergemaalEllerFremtidsfullmaktElement.append(
        String.join("\n", "  ", " \"omfang\": \"" + vergemaalEllerFremtidsfullmaktDto.getVergeEllerFullmektig().getOmfang() + "\" },"));
    vergemaalEllerFremtidsfullmaktElement.append(String.join("\n", "  ", " \"metadata\": {"));
    vergemaalEllerFremtidsfullmaktElement.append(String.join("\n", "  ", " \"opplysningsid\": \"1234\","));
    vergemaalEllerFremtidsfullmaktElement.append(String.join("\n", "  ", " \"master\": \"FREG\"}}"));

    return vergemaalEllerFremtidsfullmaktElement.toString();
  }
}
