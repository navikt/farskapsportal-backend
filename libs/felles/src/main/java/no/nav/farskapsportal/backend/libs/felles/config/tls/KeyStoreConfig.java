package no.nav.farskapsportal.backend.libs.felles.config.tls;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import no.nav.farskapsportal.backend.libs.felles.exception.CertificateException;
import no.nav.farskapsportal.backend.libs.felles.exception.KeyException;

public class KeyStoreConfig {
  public final KeyStore keyStore;
  public final String alias;
  public final String keystorePassword;
  public final String privatekeyPassword;

  public KeyStoreConfig(
      KeyStore keyStore, String alias, String keystorePassword, String privatekeyPassword) {
    this.keyStore = keyStore;
    this.alias = alias;
    this.keystorePassword = keystorePassword;
    this.privatekeyPassword = privatekeyPassword;
  }

  public Certificate[] getCertificateChain() {
    try {
      return this.keyStore.getCertificateChain(this.alias);
    } catch (KeyStoreException var2) {
      throw new KeyException(
          "Failed to retrieve certificate chain from key store. Is key store initialized?", var2);
    }
  }

  public X509Certificate getCertificate() {
    Certificate certificate;
    try {
      certificate = this.keyStore.getCertificate(this.alias);
    } catch (KeyStoreException var3) {
      throw new CertificateException(
          "Failed to retrieve certificate from key store. Is key store initialized?", var3);
    }

    if (certificate == null) {
      throw new CertificateException(
          "Failed to find certificate in key store. Are you sure a key store with a certificate is supplied and that you've given the right alias?");
    } else if (!(certificate instanceof X509Certificate)) {
      throw new CertificateException(
          "Only X509 certificates are supported. Got a certificate with type "
              + certificate.getClass().getSimpleName());
    } else {
      this.verifyCorrectAliasCasing(certificate);
      return (X509Certificate) certificate;
    }
  }

  public PrivateKey getPrivateKey() {
    try {
      Key key = this.keyStore.getKey(this.alias, this.privatekeyPassword.toCharArray());
      if (!(key instanceof PrivateKey)) {
        throw new KeyException(
            "Failed to retrieve private key from key store. Expected a PrivateKey, got "
                + key.getClass().getCanonicalName());
      } else {
        return (PrivateKey) key;
      }
    } catch (KeyStoreException var2) {
      throw new KeyException(
          "Failed to retrieve private key from key store. Is key store initialized?", var2);
    } catch (NoSuchAlgorithmException var3) {
      throw new KeyException(
          "Failed to retrieve private key from key store. Verify that the key is supported on the platform.",
          var3);
    } catch (UnrecoverableKeyException var4) {
      throw new KeyException(
          "Failed to retrieve private key from key store. Verify that the password is correct.",
          var4);
    }
  }

  /**
   * @deprecated
   */
  @Deprecated
  public static KeyStoreConfig fromKeyStore(
      InputStream javaKeyStore, String alias, String keyStorePassword, String privatekeyPassword) {
    return fromJavaKeyStore(javaKeyStore, alias, keyStorePassword, privatekeyPassword);
  }

  public static KeyStoreConfig fromJavaKeyStore(
      InputStream javaKeyStore, String alias, String keyStorePassword, String privatekeyPassword) {
    KeyStore ks = KeyStoreType.JCEKS.loadKeyStore(javaKeyStore, keyStorePassword);
    return new KeyStoreConfig(ks, alias, keyStorePassword, privatekeyPassword);
  }

  public static KeyStoreConfig fromOrganizationCertificate(
      InputStream organizationCertificateStream, String privatekeyPassword) {
    KeyStore ks =
        KeyStoreType.PKCS12.loadKeyStore(organizationCertificateStream, privatekeyPassword);

    Enumeration aliases;
    try {
      aliases = ks.aliases();
    } catch (KeyStoreException var5) {
      throw new KeyException(
          "Could not retrieve aliases from the key store, because "
              + var5.getClass().getSimpleName()
              + ": '"
              + var5.getMessage()
              + "'. Are you sure this is an organization certificate in PKCS12 format?",
          var5);
    }

    if (!aliases.hasMoreElements()) {
      throw new KeyException(
          "The keystore contains no aliases, i.e. is empty! Are you sure this is an organization certificate in PKCS12 format?");
    } else {
      String keyName = (String) aliases.nextElement();
      return new KeyStoreConfig(ks, keyName, privatekeyPassword, privatekeyPassword);
    }
  }

  private void verifyCorrectAliasCasing(Certificate certificate) {
    try {
      String aliasFromKeystore = this.keyStore.getCertificateAlias(certificate);
      if (!aliasFromKeystore.equals(this.alias)) {
        throw new CertificateException(
            "Certificate alias in keystore was not same as provided alias. Probably different casing. In keystore: "
                + aliasFromKeystore
                + ", from config: "
                + this.alias);
      }
    } catch (KeyStoreException var3) {
      throw new CertificateException(
          "Unable to get certificate alias based on certificate. This should never happen, as we just read the certificate from the same keystore.",
          var3);
    }
  }
}
