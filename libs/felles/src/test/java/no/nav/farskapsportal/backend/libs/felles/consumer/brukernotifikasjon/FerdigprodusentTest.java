package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static com.fasterxml.jackson.module.kotlin.ExtensionsKt.jacksonObjectMapper;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.UUID;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Oppgavebestilling;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.tms.varsel.action.InaktiverVarsel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Ferdigprodusent")
@SpringBootTest(classes = FarskapsportalFellesTestConfig.class)
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 0)
public class FerdigprodusentTest {

  @Value("${wiremock.server.port}")
  private String wiremockPort;

  private @Autowired FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;
  private @Autowired Ferdigprodusent ferdigprodusent;
  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @Autowired OppgavebestillingDao oppgavebestillingDao;

  private @MockBean KafkaTemplate<String, String> ferdigkoe;
  private @MockBean GcpStorageManager gcpStorageManager;

  @Test
  void skalFerdigstilleFarsSigneringsoppgave() throws Exception {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var ferdigfanger = ArgumentCaptor.forClass(String.class);

    var fnrFar = "11111122222";

    var farskapserklaeringSomVenterPaaFarsSignatur =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(6));
    farskapserklaeringSomVenterPaaFarsSignatur
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var oppgavebestilling =
        oppgavebestillingDao.save(
            Oppgavebestilling.builder()
                .opprettet(LocalDateTime.now())
                .eventId(UUID.randomUUID().toString())
                .forelder(farskapserklaering.getFar())
                .build());

    // when
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(
        Forelder.builder().foedselsnummer(fnrFar).build(), oppgavebestilling.getEventId());

    // then
    verify(ferdigkoe, times(1))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            ferdigfanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var ferdigmeldinger = ferdigfanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(ferdigmeldinger.size()).isEqualTo(1));

    var noekkel = noekler.get(0);
    var ferdigmelding = ferdigmeldinger.get(0);

    // Deserilaiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var inaktivertVarsel = objectMapper.readValue(ferdigmelding, InaktiverVarsel.class);

    assertAll(
        () -> assertThat(noekkel).isEqualTo(oppgavebestilling.getEventId()),
        () -> assertThat(inaktivertVarsel.getVarselId()).isEqualTo(oppgavebestilling.getEventId()),
        () ->
            assertThat(inaktivertVarsel.getProdusent().getCluster())
                .isEqualTo(farskapsportalFellesEgenskaper.getCluster()),
        () ->
            assertThat(inaktivertVarsel.getProdusent().getNamespace())
                .isEqualTo(farskapsportalFellesEgenskaper.getNamespace()),
        () ->
            assertThat(inaktivertVarsel.getProdusent().getAppnavn())
                .isEqualTo(farskapsportalFellesEgenskaper.getAppnavn()));
  }

  @Test
  void skalIkkeFerdigstilleOppgaveSomIkkeErAktiv() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var fnrFar = "11111122222";

    var farskapserklaeringSomVenterPaaFarsSignatur =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(6));
    farskapserklaeringSomVenterPaaFarsSignatur
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var oppgavebestilling =
        oppgavebestillingDao.save(
            Oppgavebestilling.builder()
                .opprettet(LocalDateTime.now())
                .eventId(UUID.randomUUID().toString())
                .forelder(farskapserklaering.getFar())
                .ferdigstilt(LocalDateTime.now())
                .build());

    // when
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(
        Forelder.builder().foedselsnummer(fnrFar).build(), oppgavebestilling.getEventId());

    // then
    verify(ferdigkoe, times(0)).send(anyString(), anyString(), anyString());
  }

  private Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "redirect-mor"))
                    .signeringstidspunkt(LocalDateTime.now())
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "/redirect-far"))
                    .build())
            .build();

    return Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
  }
}
