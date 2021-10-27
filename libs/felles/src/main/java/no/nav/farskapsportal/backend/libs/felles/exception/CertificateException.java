package no.nav.farskapsportal.backend.libs.felles.exception;

public class CertificateException extends ConfigurationException {
  public CertificateException(String message, Exception e) {
    super(message, e);
  }

  public CertificateException(String message) {
    super(message);
  }
}