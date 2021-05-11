package no.nav.farskapsportal.consumer.skatt;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static no.nav.farskapsportal.api.Feilkode.DOKUMENT_MANGLER_INNOHLD;
import static no.nav.farskapsportal.api.Feilkode.SKATT_OVERFOERING_FEILET;
import static no.nav.farskapsportal.api.Feilkode.XADES_FAR_UTEN_INNHOLD;
import static no.nav.farskapsportal.api.Feilkode.XADES_MOR_UTEN_INNHOLD;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.exception.SkattConsumerException;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@DisplayName("SkattConsumer")
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = FarskapsportalApplicationLocal.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public class SkattConsumerTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto UFOEDT_BARN = henteBarnUtenFnr(17);
  private static final FarskapserklaeringDto FARSKAPSERKLAERING = henteFarskapserklaeringDto(MOR, FAR, UFOEDT_BARN);

  @Autowired
  private SkattConsumer skattConsumer;

  @Autowired
  private Mapper mapper;

  @Test
  void skalIkkeKasteExceptionDersomRegistreringAvFarskapGaarIgjennomHosSkatt() {

    // given
    var farskapserklaering = mapper.toEntity(FARSKAPSERKLAERING);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    // when, then
    assertDoesNotThrow(() -> skattConsumer.registrereFarskap(farskapserklaering));
  }

  @Test
  void skalKasteExceptionDersomPdfDokumentetIkkeHarInnhold() {

    // given
    var farskapserklaering = mapper.toEntity(FARSKAPSERKLAERING);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("jadda".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(XADES_MOR_UTEN_INNHOLD);
  }

  @Test
  void skalKasteExceptionDersomMorsXadesDokumentIkkeHarInnhold() {

    // given
    var farskapserklaering = mapper.toEntity(FARSKAPSERKLAERING);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("jadda".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(XADES_FAR_UTEN_INNHOLD);
  }

  @Test
  void skalKasteExceptionDersomFarsXadesDokumentetIkkeHarInnhold() {

    // given
    var farskapserklaering = mapper.toEntity(FARSKAPSERKLAERING);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().setDokumentinnhold(Dokumentinnhold.builder().innhold("".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    // when, then
    var skattConsumerException = assertThrows(SkattConsumerException.class, () -> skattConsumer.registrereFarskap(farskapserklaering));

    assertThat(skattConsumerException.getFeilkode()).isEqualTo(DOKUMENT_MANGLER_INNOHLD);
  }
}
