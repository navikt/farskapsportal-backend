package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.BlobIdGcp;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.exception.BucketConsumerException;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BucketConsumer {

  private @Autowired FarskapsportalFellesEgenskaper fellesEgenskaper;
  private @Autowired GcpStorageWrapper gcpStorageWrapper;

  public BlobIdGcp getExistingBlobIdGcp(String bucket, String documentName) {
    var blobId = gcpStorageWrapper.getBlobId(bucket, documentName);
    return BlobIdGcp.builder()
        .bucket(blobId.getBucket())
        .name(blobId.getName())
        .generation(blobId.getGeneration())
        .build();
  }
  public byte[] getContentFromBucket(BlobIdGcp blobIdGcp) {
    return gcpStorageWrapper.getContent(BlobId.of(blobIdGcp.getBucket(), blobIdGcp.getName()));
  }

  public BlobIdGcp saveContentToBucket(
      ContentType contentType, String documentName, byte[] content) {
    var bucketName =
        ContentType.PADES.equals(contentType)
            ? fellesEgenskaper.getBucket().getPadesName()
            : fellesEgenskaper.getBucket().getXadesName();
    var blobId = BlobId.of(bucketName, documentName);

    try {
      var oppdatertBlobId = gcpStorageWrapper.updateBlob(blobId, content);
      if (oppdatertBlobId != null) {
        log.info("Ekisterende GCP storage blob ble oppdatert for dokument {}", documentName);
      } else {
        log.info("Ny GCP storage blob ble opprettet for dokument {}", documentName);
        blobId = gcpStorageWrapper.saveContentToBucket(bucketName, documentName, content);
      }
      return BlobIdGcp.builder()
          .bucket(blobId.getBucket())
          .generation(blobId.getGeneration())
          .name(blobId.getName())
          .build();
    } catch (Exception e) {
      log.error(
          "Feil ved oppdatering av {}-dokument med navn {} til bucket", contentType, documentName);
      throw new BucketConsumerException(Feilkode.INTERN_FEIL_OPPDATERING_AV_ERKLAERING, e);
    }
  }

  public enum ContentType {
    PADES,
    XADES
  }
}
