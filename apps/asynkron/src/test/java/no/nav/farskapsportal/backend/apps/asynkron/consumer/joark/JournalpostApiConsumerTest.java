package no.nav.farskapsportal.backend.apps.asynkron.consumer.joark;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.NAVN_FAR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.NAVN_MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.joark.stub.JournalpostApiStub;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.joark.api.DokumentInfo;
import no.nav.farskapsportal.backend.libs.dto.joark.api.OpprettJournalpostResponse;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonNavn;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.HentPersonSubResponse;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.pdl.stub.PdlApiStub;
import no.nav.farskapsportal.backend.libs.felles.test.stub.consumer.sts.stub.StsStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("JournalpostApiConsumer")
@ActiveProfiles(PROFILE_TEST)
@DirtiesContext
@AutoConfigureWireMock(port = 8096)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = FarskapsportalAsynkronTestApplication.class)
public class JournalpostApiConsumerTest {

  private static final String JOURNALSTATUS_INNGAAENDE_MED_DOKUMENTVARIANTER = "M";

  @Autowired
  private StsStub stsStub;

  @Autowired
  private JournalpostApiStub journalpostApiStub;

  @Autowired
  private PdlApiStub pdlApiStub;

  @Autowired
  private JournalpostApiConsumer journalpostApiConsumer;

  @Test
  void skalArkivereFarskapserklaeringIJoark() {

    // given
    var stubresponsFraJoark = OpprettJournalpostResponse.builder()
        .journalpostId("1234")
        .journalpostferdigstilt(false)
        .journalstatus(JOURNALSTATUS_INNGAAENDE_MED_DOKUMENTVARIANTER)
        .dokumenter(List.of(DokumentInfo.builder().dokumentInfoId("1").build()))
        .build();

    journalpostApiStub.runJournalpostApiStub(stubresponsFraJoark);
    stsStub.runSecurityTokenServiceStub("eyQgastewq521ga");

    List<HentPersonSubResponse> subResponsesMor = List.of(new HentPersonNavn(NAVN_MOR));
    pdlApiStub.runPdlApiHentPersonStub(subResponsesMor, MOR.getFoedselsnummer());

    List<HentPersonSubResponse> subResponsesFar = List.of(new HentPersonNavn(NAVN_FAR));
    pdlApiStub.runPdlApiHentPersonStub(subResponsesFar, MOR.getFoedselsnummer());

    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now().minusMinutes(1));
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
