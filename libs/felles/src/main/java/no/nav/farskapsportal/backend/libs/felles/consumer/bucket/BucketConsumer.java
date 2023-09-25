package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import com.google.cloud.storage.*;
import java.security.GeneralSecurityException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.farskapsportal.backend.libs.entity.BlobIdGcp;
import no.nav.farskapsportal.backend.libs.felles.exception.BucketConsumerException;
import no.nav.farskapsportal.backend.libs.felles.exception.Feilkode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BucketConsumer {

  private @Autowired GcpStorageWrapper gcpStorageWrapper;
  private @Value("${APPNAVN}") String appnavn;
  private @Value("${NAIS_CLUSTER_NAME}") String naisClusterName;

  public Optional<BlobIdGcp> getExistingBlobIdGcp(String bucket, String documentName) {
    var blobId = gcpStorageWrapper.getBlobId(bucket, documentName);
    return blobId.isEmpty()
        ? Optional.empty()
        : Optional.of(
            BlobIdGcp.builder()
                .bucket(blobId.get().getBucket())
                .name(blobId.get().getName())
                .generation(blobId.get().getGeneration())
                .build());
  }

  public byte[] getContentFromBucket(BlobIdGcp blobIdGcp) {
    try {
      return gcpStorageWrapper.getContent(
          BlobId.of(blobIdGcp.getBucket(), blobIdGcp.getName()),
          blobIdGcp.getEncryptionKeyVersion());
    } catch (GeneralSecurityException generalSecurityException) {
      log.error(
          "En sikkerhetsfeil inntraff ved henting av innhold fra dokument {} i  bøtte {}",
          blobIdGcp.getName(),
          blobIdGcp.getBucket(),
          generalSecurityException);
      return null;
    }
  }

  public BlobIdGcp saveContentToBucket(
      ContentType contentType, String documentName, byte[] content) {
    var bucketName = getBucketName(contentType);
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

  public boolean deleteContentFromBucket(BlobIdGcp blobIdGcp) {
    var dokumentBleSlettet =
        gcpStorageWrapper.deleteContentFromBucket(blobIdGcp.getBucket(), blobIdGcp.getName());
    log.info(
        "Dokument {} ble slettet fra bøtte {}: ",
        blobIdGcp.getName(),
        blobIdGcp.getBucket(),
        dokumentBleSlettet);
    return dokumentBleSlettet;
  }

  public enum ContentType {
    PADES,
    XADES
  }

  public String getBucketName(ContentType contentType) {
    return ContentType.PADES.equals(contentType)
        ? appnavn + "-" + getEnvironment() + "-pades"
        : appnavn + "-" + getEnvironment() + "-xades";
  }

  private String getEnvironment() {
    return "prod-gcp".equals(naisClusterName) ? "prod" : "dev";
  }
}