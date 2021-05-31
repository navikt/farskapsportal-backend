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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import no.nav.brukernotifikasjon.schemas.Done;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("SletteOppgave")
@SpringBootTest(classes = FarskapsportalApplicationLocal.class)
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@ActiveProfiles(PROFILE_TEST)
public class SletteOppgaveTest {

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
  private KafkaTemplate<Nokkel, Done> ferdigkoe;

  private SletteOppgave sletteOppgave;

  @BeforeEach
  void setup() {
    // Bønnen sletteOppgave er kun tilgjengelig for live-profilen for å unngå skedulert trigging av metoden under test.
    sletteOppgave = SletteOppgave.builder()
        .persistenceService(persistenceService)
        .brukernotifikasjonConsumer(brukernotifikasjonConsumer)
        .synlighetOppgaveIDager(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager())
        .build();
  }

  @Test
  void skalSletteUtloeptOppgave() {

    // given
    var tidspunktFoerTestIEpochMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
    farskapserklaeringDao.deleteAll();
    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    // Setter signeringstidspunkt til utenfor levetiden til oppgaven
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager()));
    persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig()), noekkelfanger.capture(), ferdigfanger.capture());

    var nokkel = noekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(ferdig.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(ferdig.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestIEpochMillis)
    );
  }

  @Test
  void skalIkkeSletteOppgaveSomIkkeErUtloept() {

    // given
    farskapserklaeringDao.deleteAll();
    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, BARN);
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now());

    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());

    // Setter signeringstidspunkt til innenfor levetiden til oppgaven
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(
        LocalDateTime.now().minusDays(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetOppgaveAntallDager() - 5));
    persistenceService.oppdatereFarskapserklaering(farskapserklaering);

    // when
    sletteOppgave.sletteUtloepteSigneringsoppgaver();

    // then
    verify(ferdigkoe, times(0))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig()), any(), any());
  }
}
