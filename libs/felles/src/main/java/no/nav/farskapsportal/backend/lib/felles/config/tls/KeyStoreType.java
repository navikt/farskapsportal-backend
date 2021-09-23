package no.nav.farskapsportal.backend.lib.felles.config.tls;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import no.nav.farskapsportal.backend.lib.felles.exception.KeyException;

enum KeyStoreType {
  PKCS12,
  JCEKS;

  private KeyStoreType() {
  }

  KeyStore getKeyStoreInstance() {
    try {
      return KeyStore.getInstance(this.name());
    } catch (KeyStoreException var2) {
      throw new KeyException("Unable to get key store instance of type " + this + ", because " + var2.getClass().getSimpleName() + ": '" + var2.getMessage() + "'", var2);
    }
  }

  KeyStore loadKeyStore(InputStream keyStoreStream, String keyStorePassword) {
    if (keyStoreStream == null) {
      throw new KeyException("Failed to initialize key store, because the key store stream is null. Please specify a stream with data.");
    } else {
      KeyStore ks = this.getKeyStoreInstance();

      try {
        ks.load(keyStoreStream, keyStorePassword.toCharArray());
        return ks;
      } catch (NoSuchAlgorithmException | CertificateException | IOException var5) {
        throw new KeyException("Unable to load key store instance of type " + this + ", because " + var5.getClass().getSimpleName() + ": '" + var5.getMessage() + "'", var5);
      }
    }
  }
}
