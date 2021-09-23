package no.nav.farskapsportal.backend.api.consumer.pdf;

import com.openhtmltopdf.extend.FSStream;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class ClassPathStream implements FSStream {

  private final ClassPathResource classPathResource;

  public ClassPathStream(String uri) {
    classPathResource = new ClassPathResource(uri);
  }

  @Override
  public InputStream getStream() {
    try {
      return classPathResource.getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Reader getReader() {
    try {
      return new InputStreamReader(classPathResource.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}