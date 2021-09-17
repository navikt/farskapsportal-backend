package no.nav.farskapsportal.consumer.brukernotifikasjon;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import no.nav.brukernotifikasjon.schemas.Done;
import no.nav.brukernotifikasjon.schemas.Nokkel;
import no.nav.brukernotifikasjon.schemas.Oppgave;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.config.egenskaper.FarskapsportalEgenskaper;
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
  private FarskapsportalEgenskaper farskapsportalEgenskaper;

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
        () -> assertThat(ferdigmelding.getTidspunkt()).isBetween(Instant.now().minusSeconds(5).toEpochMilli(), Instant.now().toEpochMilli()),
        () -> assertThat(noekkel.getSystembruker()).isEqualTo(farskapsportalEgenskaper.getSystembrukerBrukernavn()),
        () -> assertThat(noekkel.getEventId()).isEqualTo(idFarskapserklaering)
    );
  }

}
