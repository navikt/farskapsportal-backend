package no.nav.farskapsportal.backend.apps.api.consumer.pdf;

import com.openhtmltopdf.extend.FSStream;
import java.net.URI;
import java.net.URISyntaxException;

public class ClassPathStreamFactory implements com.openhtmltopdf.extend.FSStreamFactory {

  @Override
  public FSStream getUrl(String uri) {
    try {
      final URI fullUri = new URI(uri);
      return new ClassPathStream(fullUri.getPath());
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
  }

}
