package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import no.nav.brukernotifikasjon.schemas.Done;
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

@DisplayName("Ferdigprodusent")
@SpringBootTest(classes = FarskapsportalFellesConfig.class)
@ActiveProfiles(PROFILE_TEST)
public class FerdigprodusentTest {

  @Autowired
  private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @MockBean
  private KafkaTemplate<Nokkel, Done> ferdigkoe;

  @Autowired
  private Ferdigprodusent ferdigprodusent;

  @Test
  void skalFerdigstilleFarsSigneringsoppgave() {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(Nokkel.class);
    var ferdigfanger = ArgumentCaptor.forClass(Done.class);

    var fnrFar = "11111122222";
    var idFarskapserklaering = "1";

    // when
    ferdigprodusent.ferdigstilleFarsSigneringsoppgave(idFarskapserklaering, fnrFar);

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
        () -> assertThat(ferdigmelding.getFodselsnummer()).isEqualTo(fnrFar),
        () -> assertThat(ferdigmelding.getGrupperingsId()).isEqualTo(
            farskapsportalFellesEgenskaper.getBrukernotifikasjon().getGrupperingsidFarskap()),
        () -> assertThat(LocalDateTime.ofInstant(Instant.ofEpochMilli(ferdigmelding.getTidspunkt()), ZoneId.of("UTC")))
            .isBetween(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime().minusSeconds(2), ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime()),
        () -> assertThat(noekkel.getSystembruker()).isEqualTo(farskapsportalFellesEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(noekkel.getEventId()).isEqualTo(idFarskapserklaering)
    );
  }

}
