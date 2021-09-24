package farskapsportal.asynkron.consumer.skatt;

import static no.nav.farskapsportal.backend.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_TEST;
import static no.nav.farskapsportal.asynkron.exception.Feilkode.DOKUMENT_MANGLER_INNOHLD;
import static no.nav.farskapsportal.asynkron.exception.Feilkode.XADES_FAR_UTEN_INNHOLD;
import static no.nav.farskapsportal.asynkron.exception.Feilkode.XADES_MOR_UTEN_INNHOLD;
import static no.nav.farskapsportal.felles.TestUtils.FAR;
import static no.nav.farskapsportal.felles.TestUtils.MOR;
import static no.nav.farskapsportal.felles.TestUtils.henteBarnUtenFnr;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import no.nav.farskapsportal.FarskapsportalStubRunner;
import no.nav.farskapsportal.backend.asynkron.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.asynkron.exception.SkattConsumerException;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = {FarskapsportalStubRunner.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
public class SkattConsumerTest {

  private static final Barn UFOEDT_BARN = henteBarnUtenFnr(17);

  @Autowired
  private SkattConsumer skattConsumer;

  @Test
  void skalReturnereTidspunktForOverfoeringDersomRegistreringAvFarskapGaarIgjennomHosSkatt() {

    // given
    var farskapserklaering = Farskapserklaering.builder().mor(MOR).far(FAR).barn(UFOEDT_BARN).build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");

    // when
    var tidspunktForOverfoering = skattConsumer.registrereFarskap(farskapserklaering);

    // then
    assertAll(
        () -> assertThat(tidspunktForOverfoering.isBefore(LocalDateTime.now())),
        () -> assertThat(tidspunktForOverfoering.isAfter(LocalDateTime.now().minusMinutes(5)))
    );
  }

  @Test
  void skalKasteExceptionDersomPdfDokumentetIkkeHarInnhold() {

    // given
    var farskapserklaering = Farskapserklaering.builder().mor(MOR).far(FAR).barn(UFOEDT_BARN).build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("jadda".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(XADES_MOR_UTEN_INNHOLD);
  }

  @Test
  void skalKasteExceptionDersomMorsXadesDokumentIkkeHarInnhold() {

    // given
    var farskapserklaering = Farskapserklaering.builder().mor(MOR).far(FAR).barn(UFOEDT_BARN).build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("jadda".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(XADES_FAR_UTEN_INNHOLD);
  }

  @Test
  void skalKasteExceptionDersomFarsXadesDokumentetIkkeHarInnhold() {

    // given
    var farskapserklaering = Farskapserklaering.builder().mor(MOR).far(FAR).barn(UFOEDT_BARN).build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(DOKUMENT_MANGLER_INNOHLD);
  }
}
