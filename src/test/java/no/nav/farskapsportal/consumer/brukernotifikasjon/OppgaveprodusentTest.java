package no.nav.farskapsportal.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.FAR;
import static no.nav.farskapsportal.TestUtils.MOR;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.persistence.entity.Forelder;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class OppgaveprodusentTest {

  @Autowired
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

  @MockBean
  private KafkaTemplate<Nokkel, Oppgave> oppgavekoe;

  @Autowired
  private Oppgaveprodusent oppgaveprodusent;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private Mapper mapper;
  @Test
  void skalOppretteOppgaveForSigneringAvFarskapserklaering() throws MalformedURLException {

    // given
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
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicOppgave()), noekkelfanger.capture(), oppgavefanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var oppgaver = oppgavefanger.getAllValues();

    var oppgavebestilling = persistenceService.henteAktiveOppgaverTilForelderIFarskapserklaering(lagretFarskapserklaering.getId(), lagretFarskapserklaering.getFar()).stream().findFirst();

    assertAll(
        () -> assertThat(oppgavebestilling).isPresent(),
        () -> assertThat(oppgavebestilling.get().getFerdigstilt()).isNull(),
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(oppgaver.size()).isEqualTo(1));

    var noekkel = noekler.get(0);
    var oppgave = oppgaver.get(0);

    assertAll(
        () -> assertThat(oppgave.getFodselsnummer()).isEqualTo(far.getFoedselsnummer()),
        () -> assertThat(oppgave.getEksternVarsling()).isEqualTo(eksternVarsling),
        () -> assertThat(oppgave.getTekst()).isEqualTo(oppgavetekst),
        () -> assertThat(oppgave.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(oppgave.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave()),
        () -> assertThat(oppgave.getTidspunkt()).isBetween(Instant.now().minusSeconds(5).toEpochMilli(), Instant.now().toEpochMilli()),
        () -> assertThat(noekkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(noekkel.getEventId()).isEqualTo(oppgavebestilling.get().getEventId())

    );
  }
}
