package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static com.fasterxml.jackson.module.kotlin.ExtensionsKt.jacksonObjectMapper;
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
import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils;
import no.nav.tms.varsel.action.OpprettVarsel;
import no.nav.tms.varsel.action.Varseltype;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = FarskapsportalFellesTestConfig.class)
@ActiveProfiles(FarskapsportalFellesConfig.PROFILE_TEST)
@AutoConfigureWireMock(port = 0)
public class OppgaveprodusentTest {

  @Value("${wiremock.server.port}")
  private String wiremockPort;

  private @Autowired FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;
  private @Autowired Oppgaveprodusent oppgaveprodusent;
  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @Autowired OppgavebestillingDao oppgavebestillingDao;
  private @MockBean KafkaTemplate<String, String> oppgavekoe;
  private @MockBean GcpStorageManager gcpStorageManager;

  @Test
  void skalOppretteOppgaveForSigneringAvFarskapserklaering() throws Exception {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var oppgavefanger = ArgumentCaptor.forClass(String.class);

    var far = Forelder.builder().foedselsnummer("11111122222").build();
    var oppgavetekst = "Vennligst signer farskapserklæringen";
    var farskapsportalUrl = new URL(farskapsportalFellesEgenskaper.getUrl());
    var eventId = UUID.randomUUID().toString();

    var farskapserklaeringSomVenterPaaFarsSignatur =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            TestUtils.henteBarnUtenFnr(5));
    farskapserklaeringSomVenterPaaFarsSignatur
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var lagretFarskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    // when
    oppgaveprodusent.oppretteOppgaveForSigneringAvFarskapserklaering(
        lagretFarskapserklaering.getId(), far, oppgavetekst, eventId);

    // then
    verify(oppgavekoe, times(1))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            oppgavefanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var oppgaver = oppgavefanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(oppgaver.size()).isEqualTo(1));

    var oppgavebestilling =
        persistenceService
            .henteAktiveOppgaverTilForelderIFarskapserklaering(
                lagretFarskapserklaering.getId(), lagretFarskapserklaering.getFar())
            .stream()
            .findFirst();

    assertAll(
        () -> assertThat(oppgavebestilling).isPresent(),
        () -> assertThat(oppgavebestilling.get().getFerdigstilt()).isNull(),
        () -> assertThat(oppgavebestilling.get().getOpprettet()).isNotNull(),
        () ->
            assertThat(oppgavebestilling.get().getOpprettet())
                .isBefore(LocalDateTime.now().plusMinutes(10)),
        () ->
            assertThat(oppgavebestilling.get().getOpprettet())
                .isAfter(LocalDateTime.now().minusMinutes(10)),
        () -> assertThat(oppgavebestilling.get().getEventId()).isNotNull(),
        () -> assertThat(oppgavebestilling.get().getForelder()).isNotNull(),
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(oppgaver.size()).isEqualTo(1));

    var noekkel = noekler.getFirst();
    var oppgave = oppgaver.getFirst();

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var opprettetVarsel = objectMapper.readValue(oppgave, OpprettVarsel.class);

    assertAll(
        () -> assertThat(noekkel).isEqualTo(eventId),
        () -> assertThat(opprettetVarsel.getType()).isEqualTo(Varseltype.Oppgave),
        () -> assertThat(opprettetVarsel.getVarselId()).isEqualTo(noekkel),
        () -> assertThat(opprettetVarsel.getIdent()).isEqualTo(far.getFoedselsnummer()),
        () -> assertThat(opprettetVarsel.getLink()).isEqualTo(farskapsportalUrl.toString()),
        () -> assertThat(opprettetVarsel.getTekster().getFirst().getSpraakkode()).isEqualTo("nb"),
        () ->
            assertThat(opprettetVarsel.getTekster().getFirst().getTekst()).isEqualTo(oppgavetekst),
        () ->
            assertThat(opprettetVarsel.getSensitivitet().name())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaOppgave()),
        () -> assertThat(opprettetVarsel.getEksternVarsling()).isNotNull(),
        () ->
            assertThat(opprettetVarsel.getProdusent().getCluster())
                .isEqualTo(farskapsportalFellesEgenskaper.getCluster()),
        () ->
            assertThat(opprettetVarsel.getProdusent().getNamespace())
                .isEqualTo(farskapsportalFellesEgenskaper.getNamespace()),
        () ->
            assertThat(opprettetVarsel.getProdusent().getAppnavn())
                .isEqualTo(farskapsportalFellesEgenskaper.getAppnavn()));
  }

  @Test
  void skalIkkeOppretteDuplikatOppgavebestilling() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var oppgavetekst = "Vennligst signer farskapserklæringen";
    var eventId = UUID.randomUUID().toString();

    var farskapserklaeringSomVenterPaaFarsSignatur =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(5));
    farskapserklaeringSomVenterPaaFarsSignatur
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var lagretFarskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    persistenceService.lagreNyOppgavebestilling(
        lagretFarskapserklaering.getId(), UUID.randomUUID().toString());

    // when
    oppgaveprodusent.oppretteOppgaveForSigneringAvFarskapserklaering(
        lagretFarskapserklaering.getId(), lagretFarskapserklaering.getFar(), oppgavetekst, eventId);

    // then
    verify(oppgavekoe, times(0)).send(anyString(), anyString(), anyString());
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
