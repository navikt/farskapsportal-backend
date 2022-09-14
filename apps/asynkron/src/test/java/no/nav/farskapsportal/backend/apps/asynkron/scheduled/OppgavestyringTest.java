package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = FarskapsportalAsynkronTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class OppgavestyringTest {

  @Autowired
  private PersistenceService persistenceService;
  private @MockBean FarskapserklaeringDao farskapserklaeringDao;
  private @MockBean OppgaveApiConsumer oppgaveApiConsumer;
  private Oppgavestyring oppgavestyring;

  @BeforeEach
  void setup() {
    oppgavestyring = Oppgavestyring.builder()
        .oppgaveApiConsumer(oppgaveApiConsumer)
        .farskapserklaeringDao(farskapserklaeringDao).build();
  }

  @Disabled
  @Test
  void test() {

    // given
    var nyfoedtBarn = henteBarnMedFnr(LocalDate.now().minusDays(3), "12345");
    persistenceService.lagreNyFarskapserklaering(henteFarskapserklaering(
        henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), nyfoedtBarn, LocalDateTime.now()));

    // when
    oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    assert(true);
  }

  private Farskapserklaering henteFarskapserklaering(Forelder mor, Forelder far, Barn barn, LocalDateTime signeringstidspunktFar) {

    var dokument = Dokument.builder().navn("farskapserklaering.pdf")
        .signeringsinformasjonMor(
            Signeringsinformasjon.builder().redirectUrl(lageUrl("8080", "redirect-mor"))
                .signeringstidspunkt(signeringstidspunktFar.minusMinutes(10))
                .xadesXml("Mors signatur".getBytes(StandardCharsets.UTF_8)).build())
        .signeringsinformasjonFar(Signeringsinformasjon.builder().redirectUrl(lageUrl("8080", "/redirect-far"))
            .signeringstidspunkt(signeringstidspunktFar)
            .xadesXml("Fars signatur".getBytes(StandardCharsets.UTF_8)).build())
        .dokumentinnhold(Dokumentinnhold.builder().innhold("Jeg erklærer med dette farskap til barnet..".getBytes()).build())
        .build();

    var farskapserklaering = Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
    farskapserklaering.setFarBorSammenMedMor(true);
    farskapserklaering.setMeldingsidSkatt(LocalDateTime.now().toString());

    return farskapserklaering;
  }
}
