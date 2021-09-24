package farskapsportal.asynkron.consumer.skatt;

import static no.nav.farskapsportal.backend.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SKATT_SSL_TEST;
import static no.nav.farskapsportal.felles.TestUtils.FAR;
import static no.nav.farskapsportal.felles.TestUtils.MOR;
import static no.nav.farskapsportal.felles.TestUtils.henteBarnUtenFnr;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import no.nav.farskapsportal.asynkron.FarskapsportalAsynkronTestConfig;
import no.nav.farskapsportal.backend.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.asynkron.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_SKATT_SSL_TEST)
@SpringBootTest(classes = {SkattConsumer.class, FarskapsportalAsynkronTestConfig.class}, webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWireMock(port = 8096)
public class SkattConsumerSslTest {

  private static final Barn UFOEDT_BARN = henteBarnUtenFnr(17);

  @Autowired
  @Qualifier("sikret")
  private SkattConsumer skattConsumerSikret;

  @Autowired
  @Qualifier("usikret")
  private SkattConsumer skattConsumerUsikret;

  @Test
  void skalIkkeKasteExceptionDersomKommunikasjonMotSkattSkjerMedSikretProtokoll() {

    // given

    var farskapserklaering = Farskapserklaering.builder().mor(MOR).far(FAR).barn(UFOEDT_BARN).build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    // when, then
    Assertions.assertDoesNotThrow(() -> skattConsumerSikret.registrereFarskap(farskapserklaering));
  }

  @Test
  void skalKasteSkattConsumerExceptionDersomKommunikasjonMotSkattSkjerOverUsikretProtokoll() {

    // given
    var farskapserklaering = Farskapserklaering.builder().mor(MOR).far(FAR).barn(UFOEDT_BARN).build();

    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    // when, then
    assertThrows(SkattConsumerException.class, () -> skattConsumerUsikret.registrereFarskap(farskapserklaering));
  }
}
