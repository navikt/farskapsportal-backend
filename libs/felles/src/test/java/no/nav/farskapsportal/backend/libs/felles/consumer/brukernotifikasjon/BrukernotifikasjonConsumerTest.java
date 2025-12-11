package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static com.fasterxml.jackson.module.kotlin.ExtensionsKt.jacksonObjectMapper;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Oppgavebestilling;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.BrukernotifikasjonConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils;
import no.nav.tms.varsel.action.InaktiverVarsel;
import no.nav.tms.varsel.action.OpprettVarsel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("Teste BrukernotifikasjonConsumer")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {FarskapsportalFellesTestConfig.class, BrukernotifikasjonConfig.class})
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 0)
public class BrukernotifikasjonConsumerTest {

  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING =
      "Du har mottatt en farskapserklæring som venter på din signatur.";
  private static final String MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING =
      "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private static final String MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING =
      "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig.";
  private static final String MELDING_OM_MANGLENDE_SIGNERING =
      "Aksjon kreves: Farskapserklæring opprettet den %s for barn med %s er ikke ferdigstilt. Våre systemer mangler informasjon om at far har signert. Far må logge inn på Farskapsportal og forsøke å signere eller oppdatere status på ny. Ta kontakt med NAV ved problemer.";
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE =
      "Far har ikke signert farskapserklæringen innen fristen. Farskapserklæringen er derfor slettet. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private static final Barn BARN = TestUtils.henteBarnUtenFnr(5);

  private @Value("${wiremock.server.port}") String wiremockPort;
  private @Autowired BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @Autowired OppgavebestillingDao oppgavebestillingDao;
  private @MockitoBean KafkaTemplate<String, String> brukernotifikasjonKoe;
  private @MockitoBean GcpStorageManager gcpStorageManager;
  private @Autowired FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Test
  void skalInformereForeldreOmTilgjengeligFarskapserklaering() throws JsonProcessingException {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var beskjedfanger = ArgumentCaptor.forClass(String.class);

    // when
    brukernotifikasjonConsumer.informereForeldreOmTilgjengeligFarskapserklaering(MOR, FAR);

    // then
    verify(brukernotifikasjonKoe, times(2))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            beskjedfanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var beskjeder = beskjedfanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(2),
        () -> assertThat(beskjeder.size()).isEqualTo(2));

    var beskjedMor = beskjeder.get(0);
    var beskjedFar = beskjeder.get(1);

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var opprettetVarselMor = objectMapper.readValue(beskjedMor, OpprettVarsel.class);
    var opprettetVarselFar = objectMapper.readValue(beskjedFar, OpprettVarsel.class);

    var beskjedMorSynligFremTilDato =
        opprettetVarselMor
            .getAktivFremTil() // already a ZonedDateTime
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDate();
    var beskjedFarSynligFremTilDato =
        opprettetVarselFar
            .getAktivFremTil() // already a ZonedDateTime
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDate();

    assertAll(
        () -> assertThat(opprettetVarselMor.getIdent()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(opprettetVarselFar.getIdent()).isEqualTo(FAR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarselMor.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarselFar.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarselMor.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl() + "/oversikt"),
        () ->
            assertThat(opprettetVarselFar.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl() + "/oversikt"),
        () ->
            assertThat(beskjedMorSynligFremTilDato)
                .isEqualTo(
                    LocalDate.now()
                        .plusMonths(
                            farskapsportalFellesEgenskaper
                                .getBrukernotifikasjon()
                                .getSynlighetBeskjedAntallMaaneder())),
        () ->
            assertThat(beskjedFarSynligFremTilDato)
                .isEqualTo(
                    LocalDate.now()
                        .plusMonths(
                            farskapsportalFellesEgenskaper
                                .getBrukernotifikasjon()
                                .getSynlighetBeskjedAntallMaaneder())));
  }

  @Test
  void skalVarsleMorOmUtloeptOppgaveForSignering() throws JsonProcessingException {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var beskjedfanger = ArgumentCaptor.forClass(String.class);

    // when
    brukernotifikasjonConsumer.varsleMorOmUtgaattOppgaveForSignering(MOR);

    // then
    verify(brukernotifikasjonKoe, times(1))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            beskjedfanger.capture());

    var beskjed = beskjedfanger.getValue();

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var opprettetVarsel = objectMapper.readValue(beskjed, OpprettVarsel.class);

    assertAll(
        () -> assertThat(opprettetVarsel.getIdent()).isEqualTo(MOR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarsel.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarsel.getTekster().getFirst().getTekst())
                .isEqualTo(MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE),
        () ->
            assertThat(opprettetVarsel.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl()));
  }

  @Test
  void skalVarsleMorOgFarDersomFarAvbryterSignering() throws JsonProcessingException {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var beskjedfanger = ArgumentCaptor.forClass(String.class);

    // when
    brukernotifikasjonConsumer.varsleOmAvbruttSignering(MOR, FAR);

    // then
    verify(brukernotifikasjonKoe, times(2))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            beskjedfanger.capture());

    var alleBeskjeder = beskjedfanger.getAllValues();
    var beskjedTilMor = alleBeskjeder.get(0);
    var beskjedTilFar = alleBeskjeder.get(1);

    var alleNoekler = noekkelfanger.getAllValues();
    var noekkelTilMor = alleNoekler.get(0);
    var noekkelTilFar = alleNoekler.get(1);

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var opprettetVarselMor = objectMapper.readValue(beskjedTilMor, OpprettVarsel.class);
    var opprettetVarselFar = objectMapper.readValue(beskjedTilFar, OpprettVarsel.class);

    assertAll(
        //        () ->
        // assertThat(noekkelTilMor.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarselMor.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarselMor.getTekster().getFirst().getTekst())
                .isEqualTo(MELDING_TIL_MOR_OM_AVBRUTT_SIGNERING),
        () ->
            assertThat(opprettetVarselMor.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl()));

    assertAll(
        //        () ->
        // assertThat(noekkelTilFar.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarselFar.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarselFar.getTekster().getFirst().getTekst())
                .isEqualTo(MELDING_TIL_FAR_OM_AVBRUTT_SIGNERING),
        () ->
            assertThat(opprettetVarselFar.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl()));
  }

  @Test
  void skalVarsleForeldreOmManglendeSigneringForUfoedt() throws JsonProcessingException {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var beskjedfanger = ArgumentCaptor.forClass(String.class);
    var erklaeringOpprettetDato = LocalDate.now().minusDays(10);
    var ufoedt = henteBarnUtenFnr(5);

    // when
    brukernotifikasjonConsumer.varsleForeldreOmManglendeSignering(
        MOR, FAR, ufoedt, erklaeringOpprettetDato);

    // then
    verify(brukernotifikasjonKoe, times(2))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            beskjedfanger.capture());

    var alleBeskjeder = beskjedfanger.getAllValues();
    var beskjedTilMor = alleBeskjeder.get(0);
    var beskjedTilFar = alleBeskjeder.get(1);

    var alleNoekler = noekkelfanger.getAllValues();
    var noekkelTilMor = alleNoekler.get(0);
    var noekkelTilFar = alleNoekler.get(1);

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var opprettetVarselMor = objectMapper.readValue(beskjedTilMor, OpprettVarsel.class);
    var opprettetVarselFar = objectMapper.readValue(beskjedTilFar, OpprettVarsel.class);

    assertAll(
        //        () ->
        // assertThat(noekkelTilMor.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarselMor.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarselMor.getTekster().getFirst().getTekst())
                .isEqualTo(
                    String.format(
                        MELDING_OM_MANGLENDE_SIGNERING,
                        erklaeringOpprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        "termindato "
                            + ufoedt
                                .getTermindato()
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))),
        () ->
            assertThat(opprettetVarselMor.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl()));

    assertAll(
        //        () ->
        // assertThat(noekkelTilFar.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarselFar.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarselFar.getTekster().getFirst().getTekst())
                .isEqualTo(
                    String.format(
                        MELDING_OM_MANGLENDE_SIGNERING,
                        erklaeringOpprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        "termindato "
                            + ufoedt
                                .getTermindato()
                                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))),
        () ->
            assertThat(opprettetVarselFar.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl()));
  }

  @Test
  void skalVarsleForeldreOmManglendeSigneringForNyfoedt() throws JsonProcessingException {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var beskjedfanger = ArgumentCaptor.forClass(String.class);
    var erklaeringOpprettetDato = LocalDate.now().minusDays(10);
    var nyfoedt = henteBarnMedFnr(LocalDate.now().minusMonths(1));

    // when
    brukernotifikasjonConsumer.varsleForeldreOmManglendeSignering(
        MOR, FAR, nyfoedt, LocalDate.now().minusDays(10));

    // then
    verify(brukernotifikasjonKoe, times(2))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            beskjedfanger.capture());

    var alleBeskjeder = beskjedfanger.getAllValues();
    var beskjedTilMor = alleBeskjeder.get(0);
    var beskjedTilFar = alleBeskjeder.get(1);

    var alleNoekler = noekkelfanger.getAllValues();
    var noekkelTilMor = alleNoekler.get(0);
    var noekkelTilFar = alleNoekler.get(1);

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var opprettetVarselMor = objectMapper.readValue(beskjedTilMor, OpprettVarsel.class);
    var opprettetVarselFar = objectMapper.readValue(beskjedTilFar, OpprettVarsel.class);

    assertAll(
        //        () ->
        // assertThat(noekkelTilMor.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarselMor.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarselMor.getTekster().getFirst().getTekst())
                .isEqualTo(
                    String.format(
                        MELDING_OM_MANGLENDE_SIGNERING,
                        erklaeringOpprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        "fødselsnummer " + nyfoedt.getFoedselsnummer())),
        () ->
            assertThat(opprettetVarselMor.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl()));

    assertAll(
        //        () ->
        // assertThat(noekkelTilFar.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarselFar.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarselFar.getTekster().getFirst().getTekst())
                .isEqualTo(
                    String.format(
                        MELDING_OM_MANGLENDE_SIGNERING,
                        erklaeringOpprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        "fødselsnummer " + nyfoedt.getFoedselsnummer())),
        () ->
            assertThat(opprettetVarselFar.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl()));
  }

  @Test
  void skalOppretteOppgaveTilFarOmSignering() throws JsonProcessingException {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "redirect-mor"))
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "/redirect-far"))
                    .build())
            .build();

    var farskapserklaeringSomVenterPaaFarsSignatur =
        Farskapserklaering.builder().mor(MOR).far(FAR).barn(BARN).dokument(dokument).build();
    farskapserklaeringSomVenterPaaFarsSignatur
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var oppgavefanger = ArgumentCaptor.forClass(String.class);

    // when
    brukernotifikasjonConsumer.oppretteOppgaveTilFarOmSignering(farskapserklaering.getId(), FAR);

    // then
    verify(brukernotifikasjonKoe, times(1))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            oppgavefanger.capture());

    var nokkel = noekkelfanger.getValue();
    var oppgave = oppgavefanger.getValue();

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var opprettetVarsel = objectMapper.readValue(oppgave, OpprettVarsel.class);

    var oppgavebestillinger =
        persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(
            farskapserklaering.getId(), farskapserklaering.getFar());
    var oppgavebestilling = oppgavebestillinger.stream().findFirst();

    assertAll(
        () -> assertThat(oppgavebestilling).isPresent(),
        () -> assertThat(oppgavebestilling.get().getFerdigstilt()).isNull(),
        () -> assertThat(oppgavebestilling.get().getOpprettet()).isNotNull(),
        () -> assertThat(nokkel).isEqualTo(oppgavebestilling.get().getEventId()),
        () -> assertThat(opprettetVarsel.getIdent()).isEqualTo(FAR.getFoedselsnummer()),
        () ->
            assertThat(opprettetVarsel.getSensitivitet().toString())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaOppgave()),
        () ->
            assertThat(opprettetVarsel.getTekster().getFirst().getTekst())
                .isEqualTo(MELDING_OM_VENTENDE_FARSKAPSERKLAERING),
        () ->
            assertThat(opprettetVarsel.getLink())
                .isEqualTo(farskapsportalFellesEgenskaper.getUrl()));
  }

  @Test
  void skalIkkeOppretteSigneringsoppgaveDersomEnAlleredeEksistererForFarIFarskapserklaering() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "redirect-mor"))
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "/redirect-far"))
                    .build())
            .build();

    var farskapserklaeringSomVenterPaaFarsSignatur =
        Farskapserklaering.builder()
            .mor(henteForelder(Forelderrolle.MOR))
            .far(henteForelder(Forelderrolle.FAR))
            .barn(henteBarnUtenFnr(4))
            .dokument(dokument)
            .build();
    farskapserklaeringSomVenterPaaFarsSignatur
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    oppgavebestillingDao.save(
        Oppgavebestilling.builder()
            .forelder(farskapserklaering.getFar())
            .farskapserklaering(farskapserklaering)
            .opprettet(LocalDateTime.now())
            .eventId(UUID.randomUUID().toString())
            .build());

    // when
    brukernotifikasjonConsumer.oppretteOppgaveTilFarOmSignering(farskapserklaering.getId(), FAR);

    // then
    verify(brukernotifikasjonKoe, times(0)).send(anyString(), anyString(), anyString());
    var oppgavebestillinger =
        persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(
            farskapserklaering.getId(), farskapserklaering.getFar());
    var oppgavebestilling = oppgavebestillinger.stream().findFirst();

    assertAll(
        () -> assertThat(oppgavebestilling).isPresent(),
        () -> assertThat(oppgavebestilling.get().getFerdigstilt()).isNull(),
        () -> assertThat(oppgavebestilling.get().getOpprettet()).isNotNull());
  }

  @Test
  void skalSletteFarsSigneringsoppgave() throws JsonProcessingException {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "redirect-mor"))
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl(wiremockPort, "/redirect-far"))
                    .build())
            .build();
    var farskapserklaeringSomVenterPaaFarsSignatur =
        Farskapserklaering.builder()
            .mor(henteForelder(Forelderrolle.MOR))
            .far(henteForelder(Forelderrolle.FAR))
            .barn(TestUtils.henteBarnUtenFnr(5))
            .dokument(dokument)
            .build();
    farskapserklaeringSomVenterPaaFarsSignatur
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));

    var farskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var ferdigfanger = ArgumentCaptor.forClass(String.class);

    var eksisterendeOppgavebestilling =
        persistenceService.lagreNyOppgavebestilling(
            farskapserklaering.getId(), UUID.randomUUID().toString());

    // when
    brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(
        eksisterendeOppgavebestilling.getEventId(), FAR);

    // then
    verify(brukernotifikasjonKoe, times(1))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            ferdigfanger.capture());

    var nokkel = noekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var inaktivertVarsel = objectMapper.readValue(ferdig, InaktiverVarsel.class);

    assertAll(
        () -> assertThat(nokkel).isEqualTo(eksisterendeOppgavebestilling.getEventId()),
        () ->
            assertThat(inaktivertVarsel.getVarselId())
                .isEqualTo(eksisterendeOppgavebestilling.getEventId()));
  }
}
