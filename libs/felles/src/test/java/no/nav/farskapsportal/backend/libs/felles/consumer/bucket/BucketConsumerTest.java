package no.nav.farskapsportal.backend.libs.felles.consumer.bucket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.BlobId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import no.nav.farskapsportal.backend.libs.entity.BlobIdGcp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {"APPNAVN=farskapsportal-api"})
public class BucketConsumerTest {
  private @Mock GcpStorageManager gcpStorageManager;
  private @InjectMocks BucketConsumer bucketConsumer;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(bucketConsumer, "appnavn", "farskapsportal-api");
  }

  @Test
  void skalLagreNyttPadesdokumentTilBøtte() throws IOException, GeneralSecurityException {

    // given
    var idFarskapserklaering = 1234;
    var dokumentnavn = "fp-" + idFarskapserklaering + "-pades.pdf";
    var dokumenttekst = "Jeg erklærer farskap for denne personen".getBytes(StandardCharsets.UTF_8);
    var blobId =
        BlobId.of(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES), dokumentnavn);
    var blobIdGcp = BlobIdGcp.builder().encryptionKeyVersion(1).name(blobId.getName()).build();

    when(gcpStorageManager.updateBlob(blobId, dokumenttekst)).thenReturn(null);
    when(gcpStorageManager.saveContentToBucket(blobId.getBucket(), blobId.getName(), dokumenttekst))
        .thenReturn(blobIdGcp);

    // when
    var lagretBlob = bucketConsumer.lagrePades(idFarskapserklaering, dokumenttekst);

    // then
    assertThat(lagretBlob).isNotNull();
  }

  @Test
  void skalOppdatereEksisterendePadesdokumentIBøtte() throws GeneralSecurityException {

    // given
    var idFarskapserklaering = 1234;
    var dokumentnavn = "fp-" + idFarskapserklaering + "-pades.pdf";
    var dokumenttekst = "Jeg erklærer farskap for denne personen".getBytes(StandardCharsets.UTF_8);
    var blobId =
        BlobId.of(bucketConsumer.getBucketName(BucketConsumer.ContentType.PADES), dokumentnavn);
    var blobIdGcp =
        BlobIdGcp.builder()
            .bucket(blobId.getBucket())
            .name(blobId.getName())
            .encryptionKeyVersion(1)
            .build();

    when(gcpStorageManager.saveContentToBucket(blobId.getBucket(), blobId.getName(), dokumenttekst))
        .thenReturn(blobIdGcp);

    // when
    var lagretBlob = bucketConsumer.lagrePades(idFarskapserklaering, dokumenttekst);

    // then
    assertThat(lagretBlob).isNotNull();
  }

  @Test
  void skalLagreMorsXades() throws GeneralSecurityException {

    // given
    var idFarskapserklaering = 1234;
    var dokumentnavn = "xades-mor-" + idFarskapserklaering + ".xml";
    var dokumenttekst = "Jeg signerer farskapserklæring".getBytes(StandardCharsets.UTF_8);
    var blobId =
        BlobId.of(bucketConsumer.getBucketName(BucketConsumer.ContentType.XADES), dokumentnavn);
    var blobIdGcp =
        BlobIdGcp.builder()
            .bucket(blobId.getBucket())
            .name(blobId.getName())
            .encryptionKeyVersion(1)
            .build();

    when(gcpStorageManager.saveContentToBucket(blobId.getBucket(), blobId.getName(), dokumenttekst))
        .thenReturn(blobIdGcp);

    // when
    var lagretBlob = bucketConsumer.lagreXadesMor(idFarskapserklaering, dokumenttekst);

    // then
    assertThat(lagretBlob).isNotNull();
  }

  @Test
  void skalLagreFarsXades() throws GeneralSecurityException {

    // given
    var idFarskapserklaering = 1234;
    var dokumentnavn = "xades-far-" + idFarskapserklaering + ".xml";
    var dokumenttekst = "Jeg signerer farskapserklæring".getBytes(StandardCharsets.UTF_8);
    var blobId =
        BlobId.of(bucketConsumer.getBucketName(BucketConsumer.ContentType.XADES), dokumentnavn);
    var blobIdGcp =
        BlobIdGcp.builder()
            .bucket(blobId.getBucket())
            .name(blobId.getName())
            .encryptionKeyVersion(1)
            .build();

    when(gcpStorageManager.saveContentToBucket(blobId.getBucket(), blobId.getName(), dokumenttekst))
        .thenReturn(blobIdGcp);

    // when
    var lagretBlob = bucketConsumer.lagreXadesFar(idFarskapserklaering, dokumenttekst);

    // then
    assertThat(lagretBlob).isNotNull();
  }
}
