package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.Bucket;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import org.junit.jupiter.api.BeforeEach;
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

  private Bucket bucketProperties = new Bucket();

  @BeforeEach
  void setup() {
    bucketProperties.setPadesName("PaDES_bucketnavn");
    bucketProperties.setXadesName("XaDES_bucketnavn");
    when(fellesEgenskaper.getBucket()).thenReturn(bucketProperties);
  }

  @Test
  void skalLagreNyttDokumentTilBøtte() throws IOException {

    // given
    var dokumentnavn = "fp-1234";
    var dokumenttekst = "Jeg erklærer farskap for denne personen".getBytes(StandardCharsets.UTF_8);
    var blobId = BlobId.of(bucketProperties.getPadesName(), dokumentnavn);

    when(gcpStorageWrapper.updateBlob(blobId, dokumenttekst)).thenReturn(null);
    when(gcpStorageWrapper.saveContentToBucket(
            bucketProperties.getPadesName(), dokumentnavn, dokumenttekst))
        .thenReturn(blobId);

    // when
    var dokumentinnhold =
        bucketConsumer.saveContentToBucket(
            BucketConsumer.ContentType.PADES, dokumentnavn, dokumenttekst);

    // then
    assertThat(dokumentinnhold).isNotNull();
  }

  @Test
  void skalOppdatereEksisterendeDokumentIBøtte() {

    // given
    var dokumentnavn = "fp-1234";
    var dokumenttekst = "Jeg erklærer farskap for denne personen".getBytes(StandardCharsets.UTF_8);
    var blobId = BlobId.of(bucketProperties.getPadesName(), dokumentnavn);

    when(gcpStorageWrapper.saveContentToBucket(
            bucketProperties.getPadesName(), dokumentnavn, dokumenttekst))
        .thenReturn(blobId);

    // when
    var dokumentinnhold =
        bucketConsumer.saveContentToBucket(
            BucketConsumer.ContentType.PADES, dokumentnavn, dokumenttekst);

    // then
    assertThat(dokumentinnhold).isNotNull();
  }
}
