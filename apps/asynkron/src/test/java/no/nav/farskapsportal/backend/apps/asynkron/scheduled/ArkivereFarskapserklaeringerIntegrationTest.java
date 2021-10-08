package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.apps.asynkron.config.FarskapsportalAsynkronConfig.PROFILE_SCHEDULED_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteFarskapserklaering;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.config.ScheduledConfig;
import no.nav.farskapsportal.backend.apps.asynkron.config.egenskaper.FarskapsportalAsynkronEgenskaper;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@ActiveProfiles(PROFILE_SCHEDULED_TEST)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalAsynkronTestApplication.class)
@SpringJUnitConfig(ScheduledConfig.class)
public class ArkivereFarskapserklaeringerIntegrationTest {

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private MeldingsloggDao meldingsloggDao;

  @Autowired
  private FarskapsportalAsynkronEgenskaper farskapsportalAsynkronEgenskaper;

  @Autowired
  private PersistenceService persistenceService;

  @Test
  public void skalPlukkeOppOgSendeFerdigstilteFarskapserklaeringerTilSkatt() throws InterruptedException {

    // given
    var alleFarskapserklaeringer = farskapserklaeringDao.findAll();
    assertFalse(alleFarskapserklaeringer.iterator().hasNext());

    var sendingsklarFarskapserklaering1 =
        henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
            henteBarnMedFnr(LocalDate.now().minusMonths(1), "11111"));
    // Bare farskapserklæringer som er signert av far, og har satt meldingsIdSkatt er aktuelle for overføring
    sendingsklarFarskapserklaering1.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now().minusMinutes(10));
    sendingsklarFarskapserklaering1.setMeldingsidSkatt("4564");
    assertNull(sendingsklarFarskapserklaering1.getSendtTilSkatt());
    persistenceService.lagreNyFarskapserklaering(sendingsklarFarskapserklaering1);

    var sendingsklarFarskapserklaering2 = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnMedFnr(LocalDate.now().minusMonths(1), "22222"));
    // Bare farskapserklæringer som er signert av far, og har satt meldingsIdSkatt er aktuelle for overføring
    sendingsklarFarskapserklaering2.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now().minusMinutes(10));
    sendingsklarFarskapserklaering2.setMeldingsidSkatt("4564");
    assertNull(sendingsklarFarskapserklaering2.getSendtTilSkatt());
    persistenceService.lagreNyFarskapserklaering(sendingsklarFarskapserklaering2);

    var ikkeSendingsklarFarskapserklaering = henteFarskapserklaering(henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR),
        henteBarnMedFnr(LocalDate.now().minusMonths(1), "33333"));
    // Bare farskapserklæringer som er signert av far, og har satt meldingsIdSkatt er aktuelle for overføring
    ikkeSendingsklarFarskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusMinutes(15));
    persistenceService.lagreNyFarskapserklaering(ikkeSendingsklarFarskapserklaering);
    assertNull(ikkeSendingsklarFarskapserklaering.getSendtTilSkatt());

    // when
    Thread.sleep(farskapsportalAsynkronEgenskaper.getArkiveringsintervall() * 2);

    var fe1 = farskapserklaeringDao.findById(sendingsklarFarskapserklaering1.getId());
    var fe2 = farskapserklaeringDao.findById(sendingsklarFarskapserklaering2.getId());
    var fe3 = farskapserklaeringDao.findById(ikkeSendingsklarFarskapserklaering.getId());

    assertAll(
        () -> assertThat(fe1).isPresent(),
        () -> assertNotNull(fe1.get().getMeldingsidSkatt()),
        () -> assertNotNull(fe1.get().getSendtTilSkatt(), "Tidspunkt for oversendelse skal være satt for erklæringer som er sendt til Skatt"),
        () -> assertTrue(meldingsIdFinnesIMeldingslogg(fe1.get().getMeldingsidSkatt())),
        () -> assertThat(fe2).isPresent(),
        () -> assertNotNull(fe2.get().getSendtTilSkatt(), "Tidspunkt for oversendelse skal være satt for erklæringer som er sendt til Skatt"),
        () -> assertNotNull(fe2.get().getMeldingsidSkatt()),
        () -> assertTrue(meldingsIdFinnesIMeldingslogg(fe2.get().getMeldingsidSkatt())),
        () -> assertThat(fe3).isPresent(),
        () -> assertNull(fe3.get().getSendtTilSkatt(), "Tidspunkt for oversendelse skal IKKE være satt for erklæringer som IKKE er sendt til Skatt"),
        () -> assertNotNull(fe3.get().getMeldingsidSkatt()),
        () -> assertFalse(meldingsIdFinnesIMeldingslogg(fe3.get().getMeldingsidSkatt()),
            "Ingen innslag i oversendelseslogg for farskapserklæring som ikke er sendt til Skatt")
    );
  }

  private boolean meldingsIdFinnesIMeldingslogg(String meldingsidSkatt) {
    var meldingsloggAlleInnslag = meldingsloggDao.findAll().iterator();
    while (meldingsloggAlleInnslag.hasNext()) {
      var innslag = meldingsloggAlleInnslag.next();
      if (innslag.getMeldingsidSkatt().equals(meldingsidSkatt)) {
        return true;
      }
    }
    return false;
  }
}
