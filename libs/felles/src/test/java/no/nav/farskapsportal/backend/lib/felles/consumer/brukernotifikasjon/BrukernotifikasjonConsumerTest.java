package no.nav.farskapsportal.backend.lib.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.lib.felles.test.utils.TestUtils.FAR;
import static no.nav.farskapsportal.backend.lib.felles.test.utils.TestUtils.MOR;
import static no.nav.farskapsportal.backend.lib.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.farskapsportal.backend.lib.felles.test.utils.TestUtils;
import no.nav.farskapsportal.backend.lib.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.lib.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.lib.entity.Barn;
import no.nav.farskapsportal.backend.lib.entity.Farskapserklaering;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Teste BrukernotifikasjonConsumer")
@SpringBootTest(classes = {BrukernotifikasjonConsumer.class, FarskapserklaeringDao.class,
    FarskapsportalFellesEgenskaper.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(PROFILE_TEST)
public class BrukernotifikasjonConsumerTest {

  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Trykk her for å opprette ny farskapserklæring.";
  private static final Barn BARN = TestUtils.henteBarnUtenFnr(5);

  @Autowired
  BrukernotifikasjonConsumer brukernotifikasjonConsumer;

  @Autowired
  FarskapserklaeringDao farskapserklaeringDao;

  @MockBean
  private KafkaTemplate<Nokkel, Beskjed> beskjedkoe;

  @MockBean
  private KafkaTemplate<Nokkel, Done> ferdigkoe;

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @Test
  void skalVarsleMorOmUtloeptOppgaveForSignering() {

    // given
    farskapserklaeringDao.deleteAll();

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);

    var tidspunktFoerTestEpochMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    // when
    brukernotifikasjonConsumer.varsleMorOmUtgaattOppgaveForSignering(MOR.getFoedselsnummer());

    // then
    verify(beskjedkoe, times(1))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var nokkel = noekkelfanger.getValue();
    var beskjed = beskjedfanger.getValue();
    var tidspunktEtterTestEpocMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(beskjed.getEksternVarsling()).isTrue(),
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(MOR.getFoedselsnummer()),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjed.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestEpochMillis),
        () -> assertThat(beskjed.getTidspunkt()).isLessThanOrEqualTo(tidspunktEtterTestEpocMillis),
        () -> assertThat(beskjed.getTekst()).isEqualTo(MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalFellesEgenskaper.getUrl())
    );
  }

  @Test
  void skalSletteFarsSigneringsoppgave() {

    // given
    farskapserklaeringDao.deleteAll();
    var tidspunktFoerTestIEpochMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
    var farskapserklaeringSomVenterPaaFarsSignatur = Farskapserklaering.builder().mor(MOR).far(FAR).barn(BARN).build();
    farskapserklaeringSomVenterPaaFarsSignatur.getDokument().getSigneringsinformasjonMor()
        .setSigneringstidspunkt(LocalDateTime.now().minusMinutes(3));
    var farskapserklaering = farskapserklaeringDao.save(farskapserklaeringSomVenterPaaFarsSignatur);

    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);

    // when
    brukernotifikasjonConsumer.sletteFarsSigneringsoppgave(farskapserklaering.getId(), FAR.getFoedselsnummer());

    // then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicFerdig()), noekkelfanger.capture(), ferdigfanger.capture());

    var nokkel = noekkelfanger.getAllValues().get(0);
    var ferdig = ferdigfanger.getAllValues().get(0);

    assertAll(
        () -> assertThat(nokkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(ferdig.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(ferdig.getFodselsnummer()).isEqualTo(FAR.getFoedselsnummer()),
        () -> assertThat(ferdig.getTidspunkt()).isGreaterThanOrEqualTo(tidspunktFoerTestIEpochMillis)
    );
  }

}
