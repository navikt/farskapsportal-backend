package farskapsportal.asynkron.consumer.joark;

import static no.nav.farskapsportal.backend.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_TEST;
import static no.nav.farskapsportal.felles.TestUtils.FAR;
import static no.nav.farskapsportal.felles.TestUtils.MOR;
import static no.nav.farskapsportal.felles.TestUtils.henteBarnUtenFnr;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.JournalpostApiConsumer;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.api.DokumentInfo;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.api.OpprettJournalpostResponse;
import no.nav.farskapsportal.asynkron.consumer.joark.stub.JournalpostApiStub;
import no.nav.farskapsportal.backend.lib.felles.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.felles.consumer.sts.stub.StsStub;
import no.nav.farskapsportal.backend.lib.entity.Barn;
import no.nav.farskapsportal.backend.lib.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.lib.entity.Farskapserklaering;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("JournalpostApiConsumer")
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 8096)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {JournalpostApiConsumer.class, JournalpostApiStub.class})
public class JournalpostApiConsumerTest {

  private static final Barn UFOEDT_BARN = henteBarnUtenFnr(17);
  private static final String NAVN_MOR = "Klara Alexandra Duck";
  private static final String NAVN_FAR = "Ferdinand Pang Duck";

  private static final String JOURNALSTATUS_INNGAAENDE_MED_DOKUMENTVARIANTER = "M";

  @Autowired
  private JournalpostApiConsumer journalpostApiConsumer;

  @Autowired
  private StsStub stsStub;

  @Autowired
  private JournalpostApiStub journalpostApiStub;

  @MockBean
  private PdlApiConsumer pdlApiConsumer;

  @Test
  void skalArkivereFarskapserklaeringIJoark() {

    // given
    var stubresponsFraJoark = OpprettJournalpostResponse.builder()
        .journalpostId("1234")
        .journalpostferdigstilt(false)
        .journalstatus(JOURNALSTATUS_INNGAAENDE_MED_DOKUMENTVARIANTER)
        .dokumenter(List.of(DokumentInfo.builder().dokumentInfoId("1").build()))
        .build();

    stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");
    journalpostApiStub.runJournalpostApiStub(stubresponsFraJoark);

    var farskapserklaering = Farskapserklaering.builder().mor(MOR).far(FAR).barn(UFOEDT_BARN).build();
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now().minusMinutes(1));

    when(pdlApiConsumer.hentNavnTilPerson(MOR.getFoedselsnummer())).thenReturn(NAVN_MOR);
    when(pdlApiConsumer.hentNavnTilPerson(FAR.getFoedselsnummer())).thenReturn(NAVN_FAR);
/*
    when(personopplysningService.tilJoark(farskapserklaering)).thenReturn(
        OpprettJournalpostRequest.builder().sak(Sak.builder().sakstype(Sakstype.FAGSAK).fagsaksystem(
            Fagsaksystem.BISYS).fagsakId(saksnr).build()).build());*/

    // when
    var opprettJournalpostResponse = journalpostApiConsumer.arkivereFarskapserklaering(farskapserklaering);

    // then
    assertAll(
        () -> assertThat(opprettJournalpostResponse.getJournalpostId()).isNotNull()
    );
  }

}
