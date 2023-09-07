package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.storage.*;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.KmsEnvelopeAeadKeyManager;
import com.google.crypto.tink.integration.gcpkms.GcpKmsClient;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.BlobIdGcp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GcpStorageWrapper {

  private final String gcpKmsKeyPath;
  private final boolean krypteringPaa;
  private int keyVersion = -1;
  private final Storage storage = StorageOptions.getDefaultInstance().getService();
  private final Aead tinkClient;

  @Autowired
  public GcpStorageWrapper(
      @Value("${GCP_KMS_KEY_PATH}") String gcpKmsKeyPath,
      @Value("${farskapsportal.egenskaper.kryptering-paa}") boolean krypteringPaa)
      throws GeneralSecurityException, IOException {
    this.gcpKmsKeyPath = gcpKmsKeyPath;
    this.krypteringPaa = krypteringPaa;
    AeadConfig.register();
    log.info("Registerer GcpKmsClient med gcpKmsKeyPath {}.", gcpKmsKeyPath);
    GcpKmsClient.register(Optional.of(gcpKmsKeyPath), Optional.empty());
    fetchKeyVersion();
    tinkClient = initTinkClient();
  }

  private void fetchKeyVersion() throws IOException {
    if (!krypteringPaa) return;

    try (KeyManagementServiceClient keyManagementServiceClient =
        KeyManagementServiceClient.create()) {
      var keyName = CryptoKeyName.parse(gcpKmsKeyPath.replace("gcp-kms://", ""));
      var key = keyManagementServiceClient.getCryptoKey(keyName);
      keyVersion = Integer.parseInt(key.getPrimary().getName().split("cryptoKeyVersions/")[1]);
    }
  }

  private Aead initTinkClient() throws GeneralSecurityException {
    var handle =
        KeysetHandle.generateNew(
            KmsEnvelopeAeadKeyManager.createKeyTemplate(
                gcpKmsKeyPath, KeyTemplates.get("AES256_GCM")));

    return handle.getPrimitive(Aead.class);
  }

  public Optional<BlobId> getBlobId(String bucket, String documentName) {
    var blob = storage.get(BlobId.of(bucket, documentName));
    return blob != null ? Optional.of(blob.getBlobId()) : Optional.empty();
  }

  public byte[] getContent(BlobId blobId) throws GeneralSecurityException {
    var encryptedContent = storage.get(blobId).getContent();
    return decryptFile(encryptedContent, toBlobInfo(blobId));
  }

  public BlobIdGcp updateBlob(BlobId blobId, byte[] content)
      throws IOException, GeneralSecurityException {
    var blob = storage.get(blobId);

    if (blob == null) return null;

    WritableByteChannel channel = blob.writer();
    channel.write(ByteBuffer.wrap(encryptFile(content, toBlobInfo(blobId))));
    channel.close();

    return BlobIdGcp.builder()
        .bucket(blobId.getBucket())
        .generation(blobId.getGeneration())
        .name(blobId.getName())
        .encryptionKeyVersion(keyVersion)
        .build();
  }

  public BlobIdGcp saveContentToBucket(String bucketName, String documentName, byte[] content)
      throws GeneralSecurityException {
    var bucket = storage.get(bucketName);
    var blobId =
        bucket
            .create(
                documentName, encryptFile(content, toBlobInfo(BlobId.of(bucketName, documentName))))
            .getBlobId();
    return BlobIdGcp.builder()
        .bucket(blobId.getBucket())
        .generation(blobId.getGeneration())
        .name(blobId.getName())
        .encryptionKeyVersion(keyVersion)
        .build();
  }

  private byte[] decryptFile(byte[] file, BlobInfo blobInfo) throws GeneralSecurityException {
    if (!krypteringPaa) return file;
    // Based on example from https://cloud.google.com/kms/docs/client-side-encryption
    log.info("Dekryptrerer fil {}", blobInfo.getName());
    var associatedData = blobInfo.getBlobId().toString().getBytes(StandardCharsets.UTF_8);
    return tinkClient.decrypt(file, associatedData);
  }

  private byte[] encryptFile(byte[] file, BlobInfo blobInfo) throws GeneralSecurityException {
    if (!krypteringPaa) return file;
    // This will bind the encryption to the location of the GCS blob. That if, if you rename or
    // move the blob to a different bucket, decryption will fail.
    // See https://developers.google.com/tink/aead#associated_data.

    // Based on example from https://cloud.google.com/kms/docs/client-side-encryption
    log.info("Kryptrerer fil {}", blobInfo.getName());
    var associatedData = blobInfo.getBlobId().toString().getBytes(StandardCharsets.UTF_8);
    return tinkClient.encrypt(file, associatedData);
  }

  private BlobInfo toBlobInfo(BlobId blobId) {
    return BlobInfo.newBuilder(blobId.getBucket(), blobId.getName())
        .setContentType(MediaType.APPLICATION_PDF.getType())
        .build();
  }
}
