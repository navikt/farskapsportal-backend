package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

<<<<<<< HEAD:libs/felles/src/test/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/OppgaveprodusentTest.java
=======
import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.FAR;
import static no.nav.farskapsportal.TestUtils.MOR;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
>>>>>>> main:src/test/java/no/nav/farskapsportal/consumer/brukernotifikasjon/OppgaveprodusentTest.java
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
<<<<<<< HEAD:libs/felles/src/test/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/OppgaveprodusentTest.java
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
=======
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.util.Mapper;
>>>>>>> main:src/test/java/no/nav/farskapsportal/consumer/brukernotifikasjon/OppgaveprodusentTest.java
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
  private Mapper mapper;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private OppgavebestillingDao oppgavebestillingDao;

  @Test
  void skalOppretteOppgaveForSigneringAvFarskapserklaering() throws MalformedURLException {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var oppgavefanger = ArgumentCaptor.forClass(Oppgave.class);

    var far = Forelder.builder().foedselsnummer("11111122222").build();
    var oppgavetekst = "Vennligst signer farskapserklæringen";
    var eksternVarsling = false;

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, henteBarnUtenFnr(5));
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(3));
    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(mapper.toEntity(farskapserklaeringSomVenterPaaFarsSignatur));

    // when
    oppgaveprodusent.oppretteOppgaveForSigneringAvFarskapserklaering(lagretFarskapserklaering.getId(), far, oppgavetekst, eksternVarsling,
        new URL(farskapsportalEgenskaper.getUrl()));

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
<<<<<<< HEAD:libs/felles/src/test/java/no/nav/farskapsportal/backend/libs/felles/consumer/brukernotifikasjon/OppgaveprodusentTest.java
        () -> assertThat(noekkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(noekkel.getEventId()).isEqualTo(idFarskapserklaering)
=======
        () -> assertThat(noekkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(noekkel.getEventId()).isEqualTo(oppgavebestilling.get().getEventId())
>>>>>>> main:src/test/java/no/nav/farskapsportal/consumer/brukernotifikasjon/OppgaveprodusentTest.java

    );
  }

  @Test
  void skalIkkeOppretteDuplikatOppgavebestilling() throws MalformedURLException {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var far = Forelder.builder().foedselsnummer("11111122222").build();
    var oppgavetekst = "Vennligst signer farskapserklæringen";
    var eksternVarsling = false;

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, henteBarnUtenFnr(5));
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(3));
    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(mapper.toEntity(farskapserklaeringSomVenterPaaFarsSignatur));

    persistenceService.lagreNyOppgavebestilling(lagretFarskapserklaering.getId(), UUID.randomUUID().toString());

    // when
    oppgaveprodusent.oppretteOppgaveForSigneringAvFarskapserklaering(lagretFarskapserklaering.getId(), lagretFarskapserklaering.getFar(), oppgavetekst, eksternVarsling,
        new URL(farskapsportalEgenskaper.getUrl()));

    //then
    verify(oppgavekoe, times(0)).send(anyString(), any(Nokkel.class), any(Oppgave.class));
  }
}
