package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SCHEDULED_TEST;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

import no.nav.farskapsportal.backend.apps.asynkron.config.ScheduledConfig;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@ActiveProfiles(PROFILE_SCHEDULED_TEST)
@SpringJUnitConfig(ScheduledConfig.class)
public class ArkivereFarskapserklaeringerSchedulingTest {

  @MockBean
  private PersistenceService persistenceService;

  @Autowired
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Test
  public void skalKjoereJobbForArkiveringAvFarskapserklaeringer() throws InterruptedException {

    // given
    var arkiveringsintervall = farskapsportalAsynkronEgenskaper.getArkiveringsintervall();

    // when
    Thread.sleep(arkiveringsintervall * 2);

    // then
    verify(persistenceService, atLeast(1)).henteFarskapserklaeringerHvorFarIkkeBorSammenMedMorOgErSendtTilSkattMenIkkeJoark();
    verify(persistenceService, atLeast(1)).henteFarskapserklaeringerSomErKlareForOverfoeringTilSkatt();
  }

}
