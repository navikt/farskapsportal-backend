package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.BlobIdGcp;
import org.springframework.http.MediaType;

@Slf4j
public class GcpStorageManager {

  private final boolean krypteringPaa;
  private final EncryptionProvider encryptionProvider;
  private final Storage storage;

  public GcpStorageManager(
      EncryptionProvider encryptionProvider, Storage storage, boolean krypteringPaa)
      throws GeneralSecurityException, IOException {

    this.encryptionProvider = encryptionProvider;
    this.storage = storage;
    this.krypteringPaa = krypteringPaa;
  }

  public Optional<BlobId> getBlobId(String bucket, String documentName) {
    var blob = storage.get(BlobId.of(bucket, documentName));
    return blob != null ? Optional.of(blob.getBlobId()) : Optional.empty();
  }

  public byte[] getContent(BlobId blobId, int encryptionKeyVersion)
      throws GeneralSecurityException {
    var storedContent = storage.get(blobId).getContent();
    if (krypteringPaa && encryptionKeyVersion > 0) {
      return decryptFile(storedContent, toBlobInfo(blobId));
    } else {
      return storedContent;
    }
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
        .encryptionKeyVersion(encryptionProvider.getKeyVersion())
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
        .encryptionKeyVersion(encryptionProvider.getKeyVersion())
        .build();
  }

  public boolean deleteContentFromBucket(String bucketName, String documentName) {
    log.info("Sletter dokument {} fra bøtte {}.", bucketName, documentName);
    return storage.delete(BlobId.of(bucketName, documentName));
  }

  private byte[] decryptFile(byte[] file, BlobInfo blobInfo) throws GeneralSecurityException {

    // Based on example from https://cloud.google.com/kms/docs/client-side-encryption
    log.info("Dekryptrerer fil {}", blobInfo.getName());
    var associatedData = blobInfo.getBlobId().toString().getBytes(StandardCharsets.UTF_8);
    return encryptionProvider.decrypt(file, associatedData);
  }

  private byte[] encryptFile(byte[] file, BlobInfo blobInfo) throws GeneralSecurityException {
    if (!krypteringPaa) return file;
    // This will bind the encryption to the location of the GCS blob. That if, if you rename or
    // move the blob to a different bucket, decryption will fail.
    // See https://developers.google.com/tink/aead#associated_data.

    // Based on example from https://cloud.google.com/kms/docs/client-side-encryption
    log.info("Kryptrerer fil {}", blobInfo.getName());
    var associatedData = blobInfo.getBlobId().toString().getBytes(StandardCharsets.UTF_8);
    return encryptionProvider.encrypt(file, associatedData);
  }

  private BlobInfo toBlobInfo(BlobId blobId) {
    return BlobInfo.newBuilder(blobId.getBucket(), blobId.getName())
        .setContentType(MediaType.APPLICATION_PDF.getType())
        .build();
  }
}
