package no.nav.farskapsportal.consumer.joark;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaeringDto;
import static no.nav.farskapsportal.TestUtils.henteForelder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.api.Forelderrolle;
import no.nav.farskapsportal.consumer.joark.api.DokumentInfo;
import no.nav.farskapsportal.consumer.joark.api.OpprettJournalpostResponse;
import no.nav.farskapsportal.consumer.joark.stub.JournalpostApiStub;
import no.nav.farskapsportal.consumer.sts.stub.StsStub;
import no.nav.farskapsportal.dto.BarnDto;
import no.nav.farskapsportal.dto.FarskapserklaeringDto;
import no.nav.farskapsportal.dto.ForelderDto;
import no.nav.farskapsportal.persistence.entity.Dokumentinnhold;
import no.nav.farskapsportal.service.PersonopplysningService;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("JournalpostApiConsumer")
@ActiveProfiles(PROFILE_TEST)
@AutoConfigureWireMock(port = 8096)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = FarskapsportalApplicationLocal.class)
public class JournalpostApiConsumerTest {

  private static final ForelderDto MOR = henteForelder(Forelderrolle.MOR);
  private static final ForelderDto FAR = henteForelder(Forelderrolle.FAR);
  private static final BarnDto UFOEDT_BARN = henteBarnUtenFnr(17);
  private static final FarskapserklaeringDto FARSKAPSERKLAERING = henteFarskapserklaeringDto(MOR, FAR, UFOEDT_BARN);

  private static final String JOURNALSTATUS_INNGAAENDE_MED_DOKUMENTVARIANTER = "M";

  @Autowired
  private JournalpostApiConsumer journalpostApiConsumer;

  @Autowired
  private StsStub stsStub;

  @Autowired
  private JournalpostApiStub journalpostApiStub;

  @MockBean
  private PersonopplysningService personopplysningService;

  @Autowired
  private Mapper mapper;

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

    var farskapserklaering = mapper.toEntity(FARSKAPSERKLAERING);
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setXadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8));

    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setXadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8));
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build());
    farskapserklaering.setMeldingsidSkatt("123");
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now().minusMinutes(1));

    when(personopplysningService.henteNavn(MOR.getFoedselsnummer())).thenReturn(MOR.getNavn());
    when(personopplysningService.henteNavn(FAR.getFoedselsnummer())).thenReturn(FAR.getNavn());
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
