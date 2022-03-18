package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.BrukernotifikasjonConfig.NAMESPACE_FARSKAPSPORTAL;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Oppgavebestilling;
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.farskapsportal.backend.libs.felles.util.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Ferdigprodusent")
@SpringBootTest(classes = FarskapsportalFellesTestConfig.class)
@ActiveProfiles(PROFILE_TEST)
public class FerdigprodusentTest {

  @Autowired
  Mapper mapper;
  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;
  @MockBean
  private KafkaTemplate<NokkelInput, DoneInput> ferdigkoe;
  @Autowired
  private Ferdigprodusent ferdigprodusent;
  @Autowired
  private PersistenceService persistenceService;
  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;
  @Autowired
  private OppgavebestillingDao oppgavebestillingDao;

  @Test
  void skalFerdigstilleFarsSigneringsoppgave() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(NokkelInput.class);
    var ferdigfanger = ArgumentCaptor.forClass(DoneInput.class);

    var fnrFar = "11111122222";

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(6));
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var oppgavebestilling = oppgavebestillingDao.save(Oppgavebestilling.builder()
        .opprettet(LocalDateTime.now()).eventId(UUID.randomUUID().toString()).forelder(farskapserklaering.getFar()).build());

    // when
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(Forelder.builder().foedselsnummer(fnrFar).build(),
        new NokkelInputBuilder().withFodselsnummer(fnrFar)
            .withGrupperingsId(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
            .withAppnavn(farskapsportalFellesEgenskaper.getAppnavn()).withNamespace(NAMESPACE_FARSKAPSPORTAL)
            .withEventId(oppgavebestilling.getEventId()).build());

    //then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicFerdig()), noekkelfanger.capture(), ferdigfanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var ferdigmeldinger = ferdigfanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(ferdigmeldinger.size()).isEqualTo(1));

    var noekkel = noekler.get(0);
    var ferdigmelding = ferdigmeldinger.get(0);

    assertAll(
        () -> assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(ferdigmelding.getTidspunkt()), ZoneId.of("UTC")))
            .isBetween(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime().minusSeconds(2), ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()),
        () -> assertThat(noekkel.getEventId()).isEqualTo(oppgavebestilling.getEventId())
    );
  }

  @Test
  void skalIkkeFerdigstilleOppgaveSomIkkeErAktiv() {

    // given
    oppgavebestillingDao.deleteAll();
    farskapserklaeringDao.deleteAll();

    var fnrFar = "11111122222";

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnUtenFnr(6));
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSomVenterPaaFarsSignatur);

    var oppgavebestilling = oppgavebestillingDao.save(Oppgavebestilling.builder()
        .opprettet(LocalDateTime.now()).eventId(UUID.randomUUID().toString()).forelder(farskapserklaering.getFar()).ferdigstilt(LocalDateTime.now())
        .build());

    // when
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(Forelder.builder().foedselsnummer(fnrFar).build(),
        new NokkelInputBuilder().withFodselsnummer(fnrFar)
            .withGrupperingsId(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap())
            .withAppnavn(farskapsportalFellesEgenskaper.getAppnavn())
            .withNamespace(NAMESPACE_FARSKAPSPORTAL).withEventId(oppgavebestilling.getEventId()).build());

    //then
    verify(ferdigkoe, times(0)).send(anyString(), any(NokkelInput.class), any(DoneInput.class));
  }
}
