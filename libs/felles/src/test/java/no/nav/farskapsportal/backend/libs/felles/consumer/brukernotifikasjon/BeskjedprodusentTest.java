package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Beskjedprodusent")
@SpringBootTest(classes = FarskapsportalFellesConfig.class)
@ActiveProfiles(PROFILE_TEST)
public class BeskjedprodusentTest {

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @MockBean
  private KafkaTemplate<Nokkel, Beskjed> ferdigkoe;

  @Autowired
  private Beskjedprodusent beskjedprodusent;

  @Test
  void skalOppretteBeskjedTilBruker() throws MalformedURLException {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var beskjedfanger = ArgumentCaptor.forClass(Beskjed.class);
    var eksternVarsling = true;

    var fnrFar = "11111122222";

    var farskapsportalUrl = new URL(farskapsportalFellesEgenskaper.getUrl());

    // when
    beskjedprodusent.oppretteBeskjedTilBruker(fnrFar, "Hei på deg", eksternVarsling, farskapsportalUrl);

    //then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var beskjeder = beskjedfanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(beskjeder.size()).isEqualTo(1));

    var noekkel = noekler.get(0);
    var beskjed = beskjeder.get(0);

    assertAll(
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(fnrFar),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(beskjed.getTidspunkt()),
            ZoneId.of("UTC"))).isBetween(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime().minusSeconds(2),
            ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()),
        () -> assertThat(beskjed.getEksternVarsling()).isEqualTo(eksternVarsling),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalUrl.toString()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjed.getTekst()).isEqualTo("Hei på deg"),
        () -> assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(beskjed.getSynligFremTil()),
            ZoneId.of("UTC"))).isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochMilli(beskjed.getTidspunkt()),
                TimeZone.getDefault().toZoneId()).plusMonths(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder())
            .withHour(0)),
        () -> assertThat(noekkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn())
    );
  }

}
