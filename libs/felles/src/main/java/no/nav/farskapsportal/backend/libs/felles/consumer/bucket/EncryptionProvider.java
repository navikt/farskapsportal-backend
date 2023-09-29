package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import java.security.GeneralSecurityException;

public interface EncryptionProvider {

  int getKeyVersion();

  byte[] encrypt(byte[] fileContent, byte[] metadata) throws GeneralSecurityException;

  byte[] decrypt(byte[] fileContent, byte[] metadata) throws GeneralSecurityException;
}
