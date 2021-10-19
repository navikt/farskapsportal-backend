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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.persistence.dao.OppgavebestillingDao;
import no.nav.farskapsportal.persistence.entity.Oppgavebestilling;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Ferdigprodusent")
@SpringBootTest(classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class FerdigprodusentTest {

  @Autowired
  Mapper mapper;
  @Autowired
  private FarskapsportalEgenskaper farskapsportalEgenskaper;
  @MockBean
  private KafkaTemplate<Nokkel, Done> ferdigkoe;
  @Autowired
  private Ferdigprodusent ferdigprodusent;
  @Autowired
  private PersistenceService persistenceService;
  @Autowired
  private OppgavebestillingDao oppgavebestillingDao;

  @Test
  void skalFerdigstilleFarsSigneringsoppgave() {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);

    var fnrFar = "11111122222";

    var farskapserklaeringSomVenterPaaFarsSignatur = henteFarskapserklaeringDto(MOR, FAR, henteBarnUtenFnr(6));
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().setSignertAvMor(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering = persistenceService.lagreNyFarskapserklaering(mapper.toEntity(farskapserklaeringSomVenterPaaFarsSignatur));

    var oppgavebestilling = oppgavebestillingDao.save(Oppgavebestilling.builder()
        .opprettet(LocalDateTime.now()).eventId(UUID.randomUUID().toString()).forelder(farskapserklaering.getFar()).build());

    // when
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(fnrFar,
        new NokkelBuilder().withSystembruker("srvfarskapsportal").withEventId(oppgavebestilling.getEventId()).build());

    //then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicFerdig()), noekkelfanger.capture(), ferdigfanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var ferdigmeldinger = ferdigfanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(ferdigmeldinger.size()).isEqualTo(1));

    var noekkel = noekler.get(0);
    var ferdigmelding = ferdigmeldinger.get(0);

    assertAll(
        () -> assertThat(ferdigmelding.getFodselsnummer()).isEqualTo(fnrFar),
        () -> assertThat(ferdigmelding.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(ferdigmelding.getTidspunkt()), ZoneId.of("UTC")))
            .isBetween(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime().minusSeconds(2), ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()),
        () -> assertThat(noekkel.getEventId()).isEqualTo(oppgavebestilling.getEventId())
    );
  }

  private Nokkel oppretteNokkel() {
    var unikEventid = UUID.randomUUID().toString();
    return new NokkelBuilder().withSystembruker(farskapsportalEgenskaper.getSystembrukerBrukernavn()).withEventId(unikEventid).build();
  }
}
