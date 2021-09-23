package farskapsportal.asynkron.consumer.joark;

import static no.nav.farskapsportal.backend.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_TEST;
import static no.nav.farskapsportal.felles.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.felles.TestUtils.henteForelder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import no.nav.farskapsportal.asynkron.FarskapsportalAsynkronTestConfig;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.FarskapsportalJoarkMapper;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.api.AvsenderMottakerIdType;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.api.BrukerIdType;
import no.nav.farskapsportal.backend.asynkron.consumer.joark.api.JournalpostType;
import no.nav.farskapsportal.backend.felles.api.Forelderrolle;
import no.nav.farskapsportal.backend.lib.felles.config.RestTemplateConfig;
import no.nav.farskapsportal.felles.config.egenskaper.FarskapsportalEgenskaper;
import no.nav.farskapsportal.backend.lib.felles.consumer.pdl.PdlApiConsumer;
import no.nav.farskapsportal.backend.lib.entity.Barn;
import no.nav.farskapsportal.backend.lib.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.lib.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.lib.entity.Forelder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = {FarskapsportalAsynkronTestConfig.class, RestTemplateConfig.class, FarskapsportalJoarkMapper.class, FarskapsportalEgenskaper.class})
public class FarskapsportalJoarkMapperTest {

  private static final Forelder MOR = henteForelder(Forelderrolle.MOR);
  private static final String NAVN_MOR = "Dolly Amanda Duck";
  private static final Forelder FAR = henteForelder(Forelderrolle.FAR);
  private static final String NAVN_FAR = "Donald Aleksander Duck";
  private static final Barn UFOEDT_BARN = henteBarnUtenFnr(17);

  @Autowired
  private FarskapsportalJoarkMapper farskapsportalJoarkMapper;

  @Autowired
  private PdlApiConsumer pdlApiConsumer;

  @Test
  void skalMappeFarskapserklaeringTilRiktigeFeltIOpprettJournalpostRequest() {

    // given
    var farskapserklaering = Farskapserklaering.builder().far(FAR).mor(MOR).barn(UFOEDT_BARN).build();
    var signeringstidspunktFar = LocalDateTime.now();
    farskapserklaering.getDokument()
        .setDokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet".getBytes(StandardCharsets.UTF_8)).build());
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(signeringstidspunktFar);
    farskapserklaering.setMeldingsidSkatt("12345");

    when(pdlApiConsumer.hentNavnTilPerson(farskapserklaering.getMor().getFoedselsnummer())).thenReturn(NAVN_MOR);

    // when
    var oppretteJournalpostRequest = farskapsportalJoarkMapper.tilJoark(farskapserklaering);

    // then
    assertAll(
        () -> assertThat(oppretteJournalpostRequest.getAvsenderMottaker().getNavn()).isEqualTo(NAVN_MOR),
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
