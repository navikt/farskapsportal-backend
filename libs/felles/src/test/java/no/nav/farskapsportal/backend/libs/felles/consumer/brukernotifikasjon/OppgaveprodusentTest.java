package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = FarskapsportalFellesTestConfig.class)
@ActiveProfiles(FarskapsportalFellesConfig.PROFILE_TEST)
public class OppgaveprodusentTest {

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @MockBean
  private KafkaTemplate<Nokkel, Oppgave> oppgavekoe;

  @Autowired
  private Oppgaveprodusent oppgaveprodusent;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private OppgavebestillingDao oppgavebestillingDao;

  @Test
  void skalOppretteOppgaveForSigneringAvFarskapserklaering() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var oppgavefanger = ArgumentCaptor.forClass(Oppgave.class);

    var far = Forelder.builder().foedselsnummer("11111122222").build();
    var oppgavetekst = "Vennligst signer farskapserklæringen";
    var eksternVarsling = false;

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        TestUtils.henteBarnUtenFnr(5));
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    // when
    oppgaveprodusent.oppretteOppgaveForSigneringAvFarskapserklaering(lagretFarskapserklaering.getId(), far, oppgavetekst, eksternVarsling);

    //then
    verify(oppgavekoe, times(1))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicOppgave()), noekkelfanger.capture(), oppgavefanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var oppgaver = oppgavefanger.getAllValues();

    var oppgavebestilling = persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(lagretFarskapserklaering.getId(),
        lagretFarskapserklaering.getFar()).stream().findFirst();

    assertAll(
        () -> assertThat(oppgavebestilling).isPresent(),
        () -> assertThat(oppgavebestilling.get().getFerdigstilt()).isNull(),
        () -> assertThat(oppgavebestilling.get().getOpprettet()).isNotNull(),
        () -> assertThat(oppgavebestilling.get().getOpprettet()).isBefore(LocalDateTime.now().plusMinutes(10)),
        () -> assertThat(oppgavebestilling.get().getOpprettet()).isAfter(LocalDateTime.now().minusMinutes(10)),
        () -> assertThat(oppgavebestilling.get().getEventId()).isNotNull(),
        () -> assertThat(oppgavebestilling.get().getForelder()).isNotNull(),
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(oppgaver.size()).isEqualTo(1));

    var noekkel = noekler.get(0);
    var oppgave = oppgaver.get(0);

    assertAll(
        () -> assertThat(oppgave.getFodselsnummer()).isEqualTo(far.getFoedselsnummer()),
        () -> assertThat(oppgave.getEksternVarsling()).isEqualTo(eksternVarsling),
        () -> assertThat(oppgave.getTekst()).isEqualTo(oppgavetekst),
        () -> assertThat(oppgave.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(oppgave.getSikkerhetsnivaa()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave()),
        () -> assertThat(oppgave.getTidspunkt()).isBetween(Instant.now().minusSeconds(5).toEpochMilli(), Instant.now().toEpochMilli()),
        () -> assertThat(noekkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(noekkel.getEventId()).isEqualTo(oppgavebestilling.get().getEventId())

    );
  }

  @Test
  void skalIkkeOppretteDuplikatOppgavebestilling() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var oppgavetekst = "Vennligst signer farskapserklæringen";
    var eksternVarsling = false;

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(5));
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    persistenceService.lagreNyOppgavebestilling(lagretFarskapserklaering.getId(), UUID.randomUUID().toString());

    // when
    oppgaveprodusent.oppretteOppgaveForSigneringAvFarskapserklaering(lagretFarskapserklaering.getId(), lagretFarskapserklaering.getFar(),
        oppgavetekst, eksternVarsling);

    //then
    verify(oppgavekoe, times(0)).send(anyString(), any(Nokkel.class), any(Oppgave.class));
  }
}
