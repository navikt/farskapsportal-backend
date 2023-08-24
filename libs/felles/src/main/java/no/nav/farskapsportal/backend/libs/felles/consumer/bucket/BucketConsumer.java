package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import com.google.cloud.storage.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.exception.BucketConsumerException;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NoArgsConstructor
public class BucketConsumer {

  private @Autowired FarskapsportalFellesEgenskaper fellesEgenskaper;
  private @Autowired GcpStorageWrapper gcpStorageWrapper;

  public byte[] getContentFromBucket(BlobId blobId) {
    return gcpStorageWrapper.getContent(blobId);
  }

  public BlobId saveContentToBucket(ContentType contentType, String documentName, byte[] content) {
    var bucketName =
        ContentType.PADES.equals(contentType)
            ? fellesEgenskaper.getBucket().getPadesName()
            : fellesEgenskaper.getBucket().getXadesName();
    var blobId = BlobId.of(bucketName, documentName);

    try {
      var oppdatertBlobId = gcpStorageWrapper.updateBlob(blobId, content);
      if (oppdatertBlobId != null) {
        log.info("Ekisterende GCP storage blob ble oppdatert for dokument {}", documentName);
        return oppdatertBlobId;
      } else {
        log.info("Ny GCP storage blob ble opprettet for dokument {}", documentName);
        return gcpStorageWrapper.saveContentToBucket(bucketName, documentName, content);
      }
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
