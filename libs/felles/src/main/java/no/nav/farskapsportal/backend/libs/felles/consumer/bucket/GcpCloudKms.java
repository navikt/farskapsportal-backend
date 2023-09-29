package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.KmsEnvelopeAeadKeyManager;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Slf4j
public class GcpCloudKms implements EncryptionProvider {

    private final Aead tinkClient;
    private final String gcpKmsKeyPath;
    private int keyVersion = -1;

  public GcpCloudKms(String gcpKmsKeyPath) throws GeneralSecurityException, IOException {

        this.gcpKmsKeyPath = gcpKmsKeyPath;

        AeadConfig.register();
        log.info("Registerer GcpKmsClient med gcpKmsKeyPath {}.", gcpKmsKeyPath);
        GcpKmsClient.register(Optional.of(gcpKmsKeyPath), Optional.empty());

        tinkClient = initTinkClient();
        fetchKeyVersion();
    }

    @Override
    public int getKeyVersion() {
        return keyVersion;
    }

    @Override
    public byte[] encrypt(byte[] fileContent, byte[] metadata) throws GeneralSecurityException {
        return tinkClient.encrypt(fileContent, metadata);
    }

    @Override
    public byte[] decrypt(byte[] fileContent, byte[] metadata) throws GeneralSecurityException {
        return tinkClient.decrypt(fileContent, metadata);
    }

    private Aead initTinkClient() throws GeneralSecurityException {
        var handle =
                KeysetHandle.generateNew(
                        KmsEnvelopeAeadKeyManager.createKeyTemplate(
                                gcpKmsKeyPath, KeyTemplates.get("AES256_GCM")));

        return handle.getPrimitive(Aead.class);
    }

    private void fetchKeyVersion() throws IOException {

        try (KeyManagementServiceClient keyManagementServiceClient =
                     KeyManagementServiceClient.create()) {
            var keyName = CryptoKeyName.parse(gcpKmsKeyPath.replace("gcp-kms://", ""));
            var key = keyManagementServiceClient.getCryptoKey(keyName);
            keyVersion = Integer.parseInt(key.getPrimary().getName().split("cryptoKeyVersions/")[1]);
        }
    }
}
