package no.nav.farskapsportal.scheduled;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.consumer.brukernotifikasjon.BrukernotifikasjonConsumer;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("VarsleFarOmSigneringsoppgave")
@SpringBootTest(classes = FarskapsportalApplicationLocal.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(PROFILE_TEST)
public class VarsleFarOmSigneringsoppgaveTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto BARN = henteBarnUtenFnr(5);

  @Autowired
  private BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @MockBean
  private KafkaTemplate<Nokkel, Beskjed> beskjedkoe;

  private VarsleFarOmSigneringsoppgave varsleFarOmSigneringsoppgave;

  @BeforeEach
  void setup() {
    // Bønnen varsleFarOmSigneringsoppgave er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    varsleFarOmSigneringsoppgave = VarsleFarOmSigneringsoppgave.builder()
        .persistenceService(persistenceService)
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .farskapsportalEgenskaper(farskapsportalEgenskaper)
        .build();
  }

  @Test
  void farSkalVarslesOmVentendeSigneringsoppdragNaarVentetidenEtterAtMorHarSignertHarUtloept() {

    // given
    farskapserklaeringDao.deleteAll();
    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getAntallDagerForsinkelseEtterMorHarSignert()));
    persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    // when
    varsleFarOmSigneringsoppgave.varsleFedreOmVentendeSigneringsoppgaver();

    // then
    verify(beskjedkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var nokkel = noekkelfanger.getAllValues().get(0);
    var beskjed = beskjedfanger.getAllValues().get(0);

    var beskjedSynligFremTilDato = Instant.ofEpochMilli(beskjed.getSynligFremTil()).atZone(ZoneId.systemDefault()).toLocalDate();

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjed.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalEgenskaper.getUrl()),
        () -> assertThat(beskjedSynligFremTilDato)
            .isEqualTo(LocalDate.now().plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
    );

  }

  @Test
  void skalIkkeSendeVarselDersomIngenFarskapserklaeringerVenterPaaFarsSignatur() {

    // given
    farskapserklaeringDao.deleteAll();

    // when
    varsleFarOmSigneringsoppgave.varsleFedreOmVentendeSigneringsoppgaver();

    // then
    verify(beskjedkoe, times(0)).send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), any(), any());
  }

  @Test
  void skalIkkeSendeVarselDersomVentetidEtterMorsSignaturIkkeErUtloept() {

    // given
    farskapserklaeringDao.deleteAll();
    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    // Setter mors signeringstidspunkt til innenfor venteperiode
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getAntallDagerForsinkelseEtterMorHarSignert() - 1));
    persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    // when
    varsleFarOmSigneringsoppgave.varsleFedreOmVentendeSigneringsoppgaver();

    // then
    verify(beskjedkoe, times(0)).send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), any(), any());

  }
}
