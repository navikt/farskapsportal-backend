package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import no.nav.farskapsportal.backend.libs.entity.BlobIdGcp;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BucketConsumerTest {

  private @Mock FarskapsportalFellesEgenskaper fellesEgenskaper;
  private @Mock GcpStorageWrapper gcpStorageWrapper;
  private @InjectMocks BucketConsumer bucketConsumer;

  @Test
  void skalLagreNyttDokumentTilBøtte() throws IOException, GeneralSecurityException {

    // given
    var dokumentnavn = "fp-1234";
    var dokumenttekst = "Jeg erklærer farskap for denne personen".getBytes(StandardCharsets.UTF_8);
    var blobId =
        BlobId.of(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES), dokumentnavn);
    var blobIdGcp = BlobIdGcp.builder().encryptionKeyVersion(1).name(blobId.getName()).build();

    when(gcpStorageWrapper.updateBlob(blobId, dokumenttekst)).thenReturn(null);
    when(gcpStorageWrapper.saveContentToBucket(blobId.getBucket(), blobId.getName(), dokumenttekst))
        .thenReturn(blobIdGcp);

    // when
    var dokumentinnhold =
        bucketConsumer.saveContentToBucket(
            BucketConsumer.ContentType.PADES, dokumentnavn, dokumenttekst);

    // then
    assertThat(dokumentinnhold).isNotNull();
  }

  @Test
  void skalOppdatereEksisterendeDokumentIBøtte() throws GeneralSecurityException {

    // given
    var dokumentnavn = "fp-1234";
    var dokumenttekst = "Jeg erklærer farskap for denne personen".getBytes(StandardCharsets.UTF_8);
    var blobId =
        BlobId.of(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES), dokumentnavn);
    var blobIdGcp =
        BlobIdGcp.builder()
            .bucket(blobId.getBucket())
            .name(blobId.getName())
            .encryptionKeyVersion(1)
            .build();

    when(gcpStorageWrapper.saveContentToBucket(blobId.getBucket(), blobId.getName(), dokumenttekst))
        .thenReturn(blobIdGcp);

    // when
    var dokumentinnhold =
        bucketConsumer.saveContentToBucket(
            BucketConsumer.ContentType.PADES, dokumentnavn, dokumenttekst);

    // then
    assertThat(dokumentinnhold).isNotNull();
  }
}
