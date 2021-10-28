package no.nav.farskapsportal.backend.apps.asynkron.consumer.joark;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.NAVN_MOR;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.joark.api.AvsenderMottakerIdType;
import no.nav.farskapsportal.backend.libs.dto.joark.api.BrukerIdType;
import no.nav.farskapsportal.backend.libs.dto.joark.api.JournalpostType;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.felles.service.PersonopplysningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = FarskapsportalAsynkronTestApplication.class)
public class FarskapsportalJoarkMapperTest {

  @Autowired
  private FarskapsportalJoarkMapper farskapsportalJoarkMapper;

  @MockBean
  private PersonopplysningService personopplysningService;

  @Test
  void skalMappeFarskapserklaeringTilRiktigeFeltIOpprettJournalpostRequest() {

    // given
    var farskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), henteBarnUtenFnr(5));
    var signeringstidspunktFar = LocalDateTime.now();
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet".getBytes(StandardCharsets.UTF_8)).build());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(signeringstidspunktFar);
    farskapserklaering.setMeldingsidSkatt("12345");

    when(personopplysningService.henteNavn(farskapserklaering.getMor().getFoedselsnummer())).thenReturn(NAVN_MOR);

    // when
    var oppretteJournalpostRequest = farskapsportalJoarkMapper.tilJoark(farskapserklaering);

    // then
    assertAll(
        () -> assertThat(oppretteJournalpostRequest.getAvsenderMottaker().getNavn()).isEqualTo(NAVN_MOR.sammensattNavn()),
        () -> assertThat(oppretteJournalpostRequest.getAvsenderMottaker().getId()).isEqualTo(farskapserklaering.getMor().getFoedselsnummer()),
        () -> assertThat(oppretteJournalpostRequest.getAvsenderMottaker().getIdType()).isEqualTo(AvsenderMottakerIdType.FNR),
        () -> assertThat(oppretteJournalpostRequest.getBruker().getId()).isEqualTo(farskapserklaering.getFar().getFoedselsnummer()),
        () -> assertThat(oppretteJournalpostRequest.getBruker().getIdType()).isEqualTo(BrukerIdType.FNR),
        () -> assertThat(oppretteJournalpostRequest.getBehandlingstema()).isEqualTo("ab0322"),
        () -> assertThat(oppretteJournalpostRequest.getDatoMottatt()).isEqualTo(Date.from(
            farskapserklaering.getDokument().getSigneringsinformasjonFar().getSigneringstidspunkt().atZone(ZoneId.systemDefault()).toInstant())),
        () -> assertThat(oppretteJournalpostRequest.getJournalfoerendeEnhet()).isEqualTo("9999"),
        () -> assertThat(oppretteJournalpostRequest.getJournalpostType()).isEqualTo(JournalpostType.INNGAAENDE),
        () -> assertThat(oppretteJournalpostRequest.getTittel()).isEqualTo("Elektronisk innsendt farskapserklæring"),
        () -> assertThat(oppretteJournalpostRequest.getTema()).isEqualTo("FAR"),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().size()).isEqualTo(1),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().get(0).getTittel()).isEqualTo("Farskapserklæring"),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().get(0).getBrevkode()).isEqualTo("fe1234"),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().get(0).getDokumentKategori()).isEqualTo("ES"),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().get(0).getDokumentvarianter().size()).isEqualTo(1),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().get(0).getDokumentvarianter().get(0).getFilnavn()).isEqualTo(
            "farskapserklaering_" + farskapserklaering.getMeldingsidSkatt() + ".pdf"),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().get(0).getDokumentvarianter().get(0).getFysiskDokument()).isEqualTo(
            farskapserklaering.getDokument().getDokumentinnhold().getInnhold()),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().get(0).getDokumentvarianter().get(0).getFiltype()).isEqualTo("PDFA"),
        () -> assertThat(oppretteJournalpostRequest.getDokumenter().get(0).getDokumentvarianter().get(0).getVariantformat()).isEqualTo("ARKIV")
    );
  }
}
