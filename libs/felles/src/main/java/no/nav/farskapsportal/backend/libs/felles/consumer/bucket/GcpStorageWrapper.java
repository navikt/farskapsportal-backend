package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import com.google.cloud.storage.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import org.springframework.stereotype.Component;

@Component
public class GcpStorageWrapper {

  private final Storage storage = StorageOptions.getDefaultInstance().getService();

  public byte[] getContent(BlobId blobId) {
    return storage.get(blobId).getContent();
  }

  public BlobId updateBlob(BlobId blobId, byte[] content) throws IOException {
    var blob = storage.get(blobId);

    if (blob == null) return null;

    WritableByteChannel channel = blob.writer();
    channel.write(ByteBuffer.wrap(content));
    channel.close();

    return blob.getBlobId();
  }

  public BlobId saveContentToBucket(String bucketName, String documentName, byte[] content) {
    var bucket = storage.get(bucketName);
    return bucket.create(documentName, content).getBlobId();
  }
}
