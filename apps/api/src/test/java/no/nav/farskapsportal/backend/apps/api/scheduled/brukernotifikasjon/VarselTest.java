package no.nav.farskapsportal.backend.apps.api.scheduled.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@EnableMockOAuth2Server
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = FarskapsportalApiApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class VarselTest {

  private static final String BRUKERNOTIFIKASJON_TOPIC_BESKJED =
      "min-side.aapen-brukernotifikasjon-beskjed-v1";
  private static final String MELDING_OM_MANGLENDE_SIGNERING =
      "Aksjon kreves: Farskapserklæring opprettet den %s for barn med %s er ikke ferdigstilt. Våre systemer mangler informasjon om at far har signert. Far må logge inn på Farskapsportal og forsøke å signere eller oppdatere status på ny. Ta kontakt med NAV ved problemer.";

  private @Autowired BrukernotifikasjonConsumer brukernotifikasjonConsumer;
  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;

  @Value("${wiremock.server.port}")
  String wiremockPort;

  private Varsel varsel;

  @MockBean private KafkaTemplate<NokkelInput, BeskjedInput> beskjedkoe;

  @BeforeEach
  void setup() {

    MockitoAnnotations.openMocks(this); // without this you will get NPE

    // Bønnen varsel er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden
    // under test.
    varsel =
        Varsel.builder()
            .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
            .egenskaperBrukernotifikasjon(farskapsportalAsynkronEgenskaper.getBrukernotifikasjon())
            .persistenceService(persistenceService)
            .build();
  }

  @Test
  void
      skalSendeEksterntVarselTilBeggeForeldreneIErklaeringerDerFarHarOppdatertBorSammenInfoMenIkkeSignertErklaeringForUfoedt() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnUtenFnr(5));
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                            .getBrukernotifikasjon()
                            .getOppgavestyringsforsinkelse()
                        + 1));
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .setDokumentinnhold(
            Dokumentinnhold.builder()
                .innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
                .build());
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                        .getFarskapsportalFellesEgenskaper()
                        .getBrukernotifikasjon()
                        .getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSendtTilSignering(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                        .getBrukernotifikasjon()
                        .getOppgavestyringsforsinkelse()));
    var farskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(NokkelInput.class);
    var beskjedfanger = ArgumentCaptor.forClass(BeskjedInput.class);

    // when
    varsel.varsleOmManglendeSigneringsinfo();

    // then
    verify(beskjedkoe, times(2))
        .send(
            eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED),
            beskjednoekkelfanger.capture(),
            beskjedfanger.capture());

    var beskjednoekkel = beskjednoekkelfanger.getAllValues().get(0);
    var beskjed = beskjedfanger.getAllValues().get(0);

    var meldingstekst =
        String.format(
            MELDING_OM_MANGLENDE_SIGNERING,
            farskapserklaering
                .getDokument()
                .getSigneringsinformasjonMor()
                .getSigneringstidspunkt()
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            "termindato "
                + farskapserklaering
                    .getBarn()
                    .getTermindato()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

    assertAll(
        () -> assertThat(beskjednoekkel.getEventId()).isNotNull(),
        () -> assertThat(beskjed.getTekst()).isEqualTo(meldingstekst));
  }

  @Test
  void
      skalSendeEksterntVarselTilBeggeForeldreneIErklaeringerDerFarHarOppdatertBorSammenInfoMenIkkeSignertErklaeringForNyfoedt() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnMedFnr(LocalDate.now().minusDays(13)));
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                            .getBrukernotifikasjon()
                            .getOppgavestyringsforsinkelse()
                        + 1));
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .setDokumentinnhold(
            Dokumentinnhold.builder()
                .innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
                .build());
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                        .getFarskapsportalFellesEgenskaper()
                        .getBrukernotifikasjon()
                        .getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonFar()
        .setSendtTilSignering(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                        .getBrukernotifikasjon()
                        .getOppgavestyringsforsinkelse()));
    var farskapserklaering =
        persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(NokkelInput.class);
    var beskjedfanger = ArgumentCaptor.forClass(BeskjedInput.class);

    // when
    varsel.varsleOmManglendeSigneringsinfo();

    // then
    verify(beskjedkoe, times(2))
        .send(
            eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED),
            beskjednoekkelfanger.capture(),
            beskjedfanger.capture());

    var beskjednoekkel = beskjednoekkelfanger.getAllValues().get(0);
    var beskjed = beskjedfanger.getAllValues().get(0);

    var meldingstekst =
        String.format(
            MELDING_OM_MANGLENDE_SIGNERING,
            farskapserklaering
                .getDokument()
                .getSigneringsinformasjonMor()
                .getSigneringstidspunkt()
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            "fødselsnummer " + farskapserklaering.getBarn().getFoedselsnummer());
    assertAll(
        () -> assertThat(beskjednoekkel.getEventId()).isNotNull(),
        () -> assertThat(beskjed.getTekst()).isEqualTo(meldingstekst));
  }

  @Test
  void skalIkkeSendeVarselOmFarskapserklaeringDerFarIkkeHarOppdatertBorSammen() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnMedFnr(LocalDate.now().minusDays(13)));
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                            .getBrukernotifikasjon()
                            .getOppgavestyringsforsinkelse()
                        + 1));
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .setDokumentinnhold(
            Dokumentinnhold.builder()
                .innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
                .build());
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                        .getFarskapsportalFellesEgenskaper()
                        .getBrukernotifikasjon()
                        .getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(null);
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(null);
    persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(NokkelInput.class);
    var beskjedfanger = ArgumentCaptor.forClass(BeskjedInput.class);

    // when
    varsel.varsleOmManglendeSigneringsinfo();

    // then
    verify(beskjedkoe, times(0))
        .send(
            eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED),
            beskjednoekkelfanger.capture(),
            beskjedfanger.capture());
  }

  @Test
  void skalIkkeSendeVarselForDeaktivertFarskapserklaering() {

    // given
    farskapserklaeringDao.deleteAll();

    var farskapserklaeringSomManglerSigneringsstatus =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            henteBarnMedFnr(LocalDate.now().minusDays(13)));
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                            .getBrukernotifikasjon()
                            .getOppgavestyringsforsinkelse()
                        + 1));
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .setDokumentinnhold(
            Dokumentinnhold.builder()
                .innhold("Jeg erklærer med dette farskap til barnet..".getBytes())
                .build());
    farskapserklaeringSomManglerSigneringsstatus
        .getDokument()
        .getSigneringsinformasjonMor()
        .setSigneringstidspunkt(
            LocalDateTime.now()
                .minusDays(
                    farskapsportalAsynkronEgenskaper
                        .getFarskapsportalFellesEgenskaper()
                        .getBrukernotifikasjon()
                        .getLevetidOppgaveAntallDager()));
    farskapserklaeringSomManglerSigneringsstatus.setDeaktivert(LocalDateTime.now());
    farskapserklaeringSomManglerSigneringsstatus.setFarBorSammenMedMor(true);
    persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomManglerSigneringsstatus);
    var beskjednoekkelfanger = ArgumentCaptor.forClass(NokkelInput.class);
    var beskjedfanger = ArgumentCaptor.forClass(BeskjedInput.class);

    // when
    varsel.varsleOmManglendeSigneringsinfo();

    // then
    verify(beskjedkoe, times(0))
        .send(
            eq(BRUKERNOTIFIKASJON_TOPIC_BESKJED),
            beskjednoekkelfanger.capture(),
            beskjedfanger.capture());
  }

  public Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn) {

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
