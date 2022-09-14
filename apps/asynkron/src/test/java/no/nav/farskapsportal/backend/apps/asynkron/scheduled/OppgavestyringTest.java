package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;

import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = FarskapsportalAsynkronTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class OppgavestyringTest {

  @MockBean
  private FarskapserklaeringDao farskapserklaeringDao;
  @MockBean
  private OppgaveApiConsumer oppgaveApiConsumer;
  private Oppgavestyring oppgavestyring;

  @BeforeEach
  void setup() {
  oppgavestyring = Oppgavestyring.builder()
      .oppgaveApiConsumer(oppgaveApiConsumer)
      .farskapserklaeringDao(farskapserklaeringDao).build();
  }

  @Test
  void test() {
    oppgavestyring.vurdereOpprettelseAvOppgave();
  }
}
