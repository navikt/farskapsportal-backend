package no.nav.farskapsportal.backend.libs.felles.consumer.brukernotifikasjon;

import static com.fasterxml.jackson.module.kotlin.ExtensionsKt.jacksonObjectMapper;
import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URL;
import java.util.UUID;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.felles.FarskapsportalFellesTestConfig;
import no.nav.farskapsportal.backend.libs.felles.config.egenskaper.FarskapsportalFellesEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.tms.varsel.action.OpprettVarsel;
import no.nav.tms.varsel.action.Varseltype;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Beskjedprodusent")
@SpringBootTest(classes = FarskapsportalFellesTestConfig.class)
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 0)
public class BeskjedprodusentTest {

  @Autowired private Beskjedprodusent beskjedprodusent;

  @Autowired private FarskapsportalFellesEgenskaper farskapsportalFellesEgenskaper;

  @MockBean private KafkaTemplate<String, String> ferdigkoe;

  @MockBean private GcpStorageManager gcpStorageManager;

  @Test
  void skalOppretteBeskjedTilBruker() throws Exception {

    // given
    var noekkelfanger = ArgumentCaptor.forClass(String.class);
    var beskjedfanger = ArgumentCaptor.forClass(String.class);
    var eksternVarsling = true;

    var far = Forelder.builder().foedselsnummer("11111122222").build();
    var farskapsportalUrl = new URL(farskapsportalFellesEgenskaper.getUrl());
    var varselId = UUID.randomUUID().toString();

    // when
    beskjedprodusent.oppretteBeskjedTilBruker(
        far, "Hei på deg", eksternVarsling, varselId, far.getFoedselsnummer());

    // then
    verify(ferdigkoe, times(1))
        .send(
            eq(farskapsportalFellesEgenskaper.getBrukernotifikasjon().getTopicBrukernotifikasjon()),
            noekkelfanger.capture(),
            beskjedfanger.capture());

    var noekler = noekkelfanger.getAllValues();
    var beskjeder = beskjedfanger.getAllValues();

    assertAll(
        () -> assertThat(noekler.size()).isEqualTo(1),
        () -> assertThat(beskjeder.size()).isEqualTo(1));

    var noekkel = noekler.getFirst();
    var beskjed = beskjeder.getFirst();

    // Deserialiserer JSON tilbake til OpprettVarsel
    var objectMapper = jacksonObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var opprettetVarsel = objectMapper.readValue(beskjed, OpprettVarsel.class);

    assertAll(
        () -> assertThat(noekkel).isEqualTo(varselId),
        () -> assertThat(opprettetVarsel.getType()).isEqualTo(Varseltype.Beskjed),
        () -> assertThat(opprettetVarsel.getVarselId()).isEqualTo(noekkel),
        () -> assertThat(opprettetVarsel.getIdent()).isEqualTo(far.getFoedselsnummer()),
        () -> assertThat(opprettetVarsel.getLink()).isEqualTo(farskapsportalUrl.toString()),
        () -> assertThat(opprettetVarsel.getTekster().getFirst().getSpraakkode()).isEqualTo("nb"),
        () ->
            assertThat(opprettetVarsel.getTekster().getFirst().getTekst()).isEqualTo("Hei på deg"),
        () -> assertThat(opprettetVarsel.getEksternVarsling()).isNull(),
        () ->
            assertThat(opprettetVarsel.getSensitivitet().name())
                .isEqualTo(
                    farskapsportalFellesEgenskaper
                        .getBrukernotifikasjon()
                        .getSikkerhetsnivaaBeskjed()),
        () ->
            assertThat(opprettetVarsel.getProdusent().getCluster())
                .isEqualTo(farskapsportalFellesEgenskaper.getCluster()),
        () ->
            assertThat(opprettetVarsel.getProdusent().getNamespace())
                .isEqualTo(farskapsportalFellesEgenskaper.getNamespace()),
        () ->
            assertThat(opprettetVarsel.getProdusent().getAppnavn())
                .isEqualTo(farskapsportalFellesEgenskaper.getAppnavn()));
  }
}
