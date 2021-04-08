package no.nav.farskapsportal.scheduled;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static no.nav.farskapsportal.TestUtils.FAR;
import static no.nav.farskapsportal.TestUtils.MOR;
import static no.nav.farskapsportal.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.TestUtils.henteFarskapserklaering;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import no.nav.farskapsportal.FarskapsportalApplicationLocal;
import no.nav.farskapsportal.consumer.skatt.SkattConsumer;
import no.nav.farskapsportal.exception.SkattConsumerException;
import no.nav.farskapsportal.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.persistence.dao.ForelderDao;
import no.nav.farskapsportal.persistence.dao.MeldingsloggDao;
import no.nav.farskapsportal.service.PersistenceService;
import no.nav.farskapsportal.util.Mapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("OverfoereTilSkatt")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = FarskapsportalApplicationLocal.class)
@ActiveProfiles(PROFILE_TEST)
public class OverfoereTilSkattTest {

  @MockBean
  private SkattConsumer skattConsumer;

  @Autowired
  private PersistenceService persistenceService;

  @Autowired
  private OverfoereTilSkatt overfoereTilSkatt;

  @Autowired
  private FarskapserklaeringDao farskapserklaeringDao;

  @Autowired
  private ForelderDao forelderDao;

  @Autowired
  private MeldingsloggDao meldingsloggDao;

  @Autowired
  private Mapper mapper;

  @Test
  void skalOppdatereMeldingsloggVedOverfoeringTilSkatt() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // given
    var farskapserklaering = mapper.toEntity(henteFarskapserklaering(MOR, FAR, henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.setMeldingsidSkatt(1234l);
    var lagretSignertFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    assert (lagretSignertFarskapserklaering.getSendtTilSkatt() == null);

    doNothing().when(skattConsumer).registrereFarskap(farskapserklaering);

    // when
    overfoereTilSkatt.vurdereOverfoeringTilSkatt();

    // then
    var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretSignertFarskapserklaering.getId());
    var logginnslag = meldingsloggDao.findAll();

    assertAll(
        () -> assertThat(oppdatertFarskapserklaering).isPresent(),
        () -> assertThat(logginnslag.iterator().next().getMeldingsidSkatt()).isEqualTo(oppdatertFarskapserklaering.get().getMeldingsidSkatt()),
        () -> assertThat(logginnslag.iterator().next().getTidspunktForOversendelse().isEqual(oppdatertFarskapserklaering.get().getSendtTilSkatt()))
    );
  }

  @Test
  void skalSetteTidspunktForOverfoeringVedOverfoeringTilSkatt() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // given
    var farskapserklaering1 = mapper.toEntity(henteFarskapserklaering(MOR, FAR, henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")));
    farskapserklaering1.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
    farskapserklaering1.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering1.setMeldingsidSkatt(1234l);
    var lagretSignertFarskapserklaering1 = persistenceService.lagreNyFarskapserklaering(farskapserklaering1);
    assert (lagretSignertFarskapserklaering1.getSendtTilSkatt() == null);

    var farskapserklaering2 = mapper.toEntity(henteFarskapserklaering(MOR, FAR, henteBarnMedFnr(LocalDate.now().minusWeeks(3), "22222")));
    farskapserklaering2.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
    farskapserklaering2.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering2.setMeldingsidSkatt(2345l);
    var lagretSignertFarskapserklaering2 = persistenceService.lagreNyFarskapserklaering(farskapserklaering2);
    assert (lagretSignertFarskapserklaering2.getSendtTilSkatt() == null);

    doNothing().when(skattConsumer).registrereFarskap(farskapserklaering1);
    doNothing().when(skattConsumer).registrereFarskap(farskapserklaering2);

    // when
    overfoereTilSkatt.vurdereOverfoeringTilSkatt();

    // then
    var oppdatertFarskapserklaering1 = farskapserklaeringDao.findById(lagretSignertFarskapserklaering1.getId());

    var oppdatertFarskapserklaering2 = farskapserklaeringDao.findById(lagretSignertFarskapserklaering2.getId());

    assertAll(
        () -> assertThat(oppdatertFarskapserklaering1).isPresent(),
        () -> assertThat(oppdatertFarskapserklaering1.get().getSendtTilSkatt()).isNotNull(),
        () -> assertThat(oppdatertFarskapserklaering2).isPresent(),
        () -> assertThat(oppdatertFarskapserklaering2.get().getSendtTilSkatt()).isNotNull()
    );
  }

  @Test
  void skalIkkeOverfoereErklaeringSomIkkeErSignertAvBeggeParter() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // given
    var farskapserklaeringIkkeSignertAvFar = mapper
        .toEntity(henteFarskapserklaering(MOR, FAR, henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")));
    farskapserklaeringIkkeSignertAvFar.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
    var lagretFarskapserklaeringIkkeSignertAvFar = persistenceService.lagreNyFarskapserklaering(farskapserklaeringIkkeSignertAvFar);
    assert (lagretFarskapserklaeringIkkeSignertAvFar.getSendtTilSkatt() == null);

    var farskapserklaeringSignertAvBeggeParter = mapper
        .toEntity(henteFarskapserklaering(MOR, FAR, henteBarnMedFnr(LocalDate.now().minusWeeks(3), "22222")));
    farskapserklaeringSignertAvBeggeParter.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
    farskapserklaeringSignertAvBeggeParter.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaeringSignertAvBeggeParter.setMeldingsidSkatt(2345l);
    var lagretFarskapserklaeringSignertAvBeggeParter = persistenceService.lagreNyFarskapserklaering(farskapserklaeringSignertAvBeggeParter);
    assert (lagretFarskapserklaeringSignertAvBeggeParter.getSendtTilSkatt() == null);

    doNothing().when(skattConsumer).registrereFarskap(farskapserklaeringSignertAvBeggeParter);

    // when
    overfoereTilSkatt.vurdereOverfoeringTilSkatt();

    // then
    var farskapserklaeringIkkeSendtTilSkatt = farskapserklaeringDao.findById(lagretFarskapserklaeringIkkeSignertAvFar.getId());
    var farskapserklaeringSendtTilSkatt = farskapserklaeringDao.findById(lagretFarskapserklaeringSignertAvBeggeParter.getId());
    var logginnslag = meldingsloggDao.findAll();

    assertAll(
        () -> assertThat(farskapserklaeringIkkeSendtTilSkatt).isPresent(),
        () -> assertThat(farskapserklaeringIkkeSendtTilSkatt.get().getMeldingsidSkatt()).isEqualTo(0L),
        () -> assertThat(farskapserklaeringIkkeSendtTilSkatt.get().getSendtTilSkatt()).isNull(),
        () -> assertThat(farskapserklaeringSendtTilSkatt).isPresent(),
        () -> assertThat(logginnslag.iterator()).hasNext(),
        () -> assertThat(logginnslag.iterator().next().getTidspunktForOversendelse())
            .isEqualTo(farskapserklaeringSendtTilSkatt.get().getSendtTilSkatt()),
        () -> assertThat(logginnslag.iterator().next().getMeldingsidSkatt()).isEqualTo(farskapserklaeringSendtTilSkatt.get().getMeldingsidSkatt())
    );
  }

  @Test
  void skalKasteSkattConsumerExceptionDersomDetOppstaarFeilIKommunikasjonMedSkatt() {

    // rydde testdata
    farskapserklaeringDao.deleteAll();
    forelderDao.deleteAll();
    meldingsloggDao.deleteAll();

    // given
    var farskapserklaering = mapper.toEntity(henteFarskapserklaering(MOR, FAR, henteBarnMedFnr(LocalDate.now().minusWeeks(3), "11111")));
    farskapserklaering.getDokument().getSigneringsinformasjonMor().setSigneringstidspunkt(LocalDateTime.now().minusHours(1));
    farskapserklaering.getDokument().getSigneringsinformasjonFar().setSigneringstidspunkt(LocalDateTime.now());
    farskapserklaering.setMeldingsidSkatt(1234l);
    var lagretSignertFarskapserklaering1 = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    assert (lagretSignertFarskapserklaering1.getSendtTilSkatt() == null);

    doThrow(SkattConsumerException.class).when(skattConsumer).registrereFarskap(farskapserklaering);

    // when, then
    assertThrows(SkattConsumerException.class, () -> overfoereTilSkatt.vurdereOverfoeringTilSkatt());
  }
}
