package no.nav.farskapsportal.backend.lib.felles.exception;

public class CertificateException extends ConfigurationException {
  public CertificateException(String message, Exception e) {
    super(message, e);
  }

  public CertificateException(String message) {
    super(message);
  }
}