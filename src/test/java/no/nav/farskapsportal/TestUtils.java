package no.nav.farskapsportal;

import java.net.URI;
import java.net.URISyntaxException;

public class TestUtils {

  public static URI lageUrl(String kontekst) {
    try {
      return new URI("https://esignering.no/" + kontekst);
    } catch (URISyntaxException uriSyntaxException) {
      throw new RuntimeException("Feil syntaks i test URI");
    }
  }
}
