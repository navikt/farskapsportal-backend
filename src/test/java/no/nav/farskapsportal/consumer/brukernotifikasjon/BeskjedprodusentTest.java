package no.nav.farskapsportal.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
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
import java.util.UUID;
import no.nav.brukernotifikasjon.schemas.Beskjed;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.builders.NokkelBuilder;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.persistence.entity.Forelder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Beskjedprodusent")
@SpringBootTest(classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class BeskjedprodusentTest {

  @Autowired
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

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

    var far = Forelder.builder().foedselsnummer("11111122222").build();

    var farskapsportalUrl = new URL(farskapsportalEgenskaper.getUrl());

    // when
    beskjedprodusent.oppretteBeskjedTilBruker(far, "Hei på deg", eksternVarsling, farskapsportalUrl, oppretteNokkel());

    //then
    verify(ferdigkoe, times(1))
        .send(eq(farskapsportalEgenskaper.getBrukernotifikasjon().getTopicBeskjed()), noekkelfanger.capture(), beskjedfanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var beskjeder = beskjedfanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(beskjeder.size()).isEqualTo(1));

    var noekkel = noekler.get(0);
    var beskjed = beskjeder.get(0);

    assertAll(
        () -> assertThat(beskjed.getFodselsnummer()).isEqualTo(far.getFoedselsnummer()),
        () -> assertThat(beskjed.getGrupperingsId()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(beskjed.getTidspunkt()),
            ZoneId.of("UTC"))).isBetween(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime().minusSeconds(2),
            ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()),
        () -> assertThat(beskjed.getEksternVarsling()).isEqualTo(eksternVarsling),
        () -> assertThat(beskjed.getLink()).isEqualTo(farskapsportalUrl.toString()),
        () -> assertThat(beskjed.getSikkerhetsnivaa()).isEqualTo(farskapsportalEgenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()),
        () -> assertThat(beskjed.getTekst()).isEqualTo("Hei på deg"),
        () -> assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(beskjed.getSynligFremTil()),
            ZoneId.of("UTC"))).isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochMilli(beskjed.getTidspunkt()),
                TimeZone.getDefault().toZoneId()).plusMonths(farskapsportalEgenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder())
            .withHour(0)),
        () -> assertThat(noekkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn())
    );
  }

  private Nokkel oppretteNokkel() {
    var unikEventid = UUID.randomUUID().toString();
    return new NokkelBuilder().withSystembruker(farskapsportalEgenskaper.getSystembrukerBrukernavn()).withEventId(unikEventid).build();
  }
}
