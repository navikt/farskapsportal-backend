package no.nav.farskapsportal.backend.apps.api.scheduled.oppgave;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import no.nav.farskapsportal.backend.apps.api.FarskapsportalApiApplicationLocal;
import no.nav.farskapsportal.backend.apps.api.config.egenskaper.Oppgave;
import no.nav.farskapsportal.backend.apps.api.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.apps.api.service.PersonopplysningService;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.oppgave.Oppgaveforespoersel;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.consumer.bucket.GcpStorageManager;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@DirtiesContext
@EnableMockOAuth2Server
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(
    classes = FarskapsportalApiApplicationLocal.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class OppgavestyringTest {

  private static final String OPPGAVE_DATOFORMAT_I_BESKRIVELSE = "dd.MM.YYYY";

  private static final String OPPGAVEBESKRIVELSE_GENERELL =
      "ELEKTRONISK ERKLÆRING -"
          + " Farskap for %s er erklært elektronisk. Far (%s) har oppgitt at han ikke bor sammen med mor (%s). Vurder om det skal tas opp bidragssak.";

  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @MockBean OppgaveApiConsumer oppgaveApiConsumer;
  private @MockBean Oppgave egenskaperOppgave;
  private @MockBean PersonopplysningService personopplysningService;
  private @MockBean GcpStorageManager gcpStorageManager;
  private Oppgavestyring oppgavestyring;

  @BeforeEach
  void setup() {
    farskapserklaeringDao.deleteAll();

    oppgavestyring =
        Oppgavestyring.builder()
            .egenskaperOppgavestyring(egenskaperOppgave)
            .oppgaveApiConsumer(oppgaveApiConsumer)
            .farskapserklaeringDao(farskapserklaeringDao)
            .personopplysningService(personopplysningService)
            .build();
  }

  @Test
  @DisplayName("Skal respektere daglig maksimumsgrense")
  void skalRespektereDagligMaksgrense() {

    // given
    var maksAntallErklaeringer = 2;
    var mor1sAktoerid = "40506070809010";
    var barn1 = henteBarnMedFnr(LocalDate.now().minusDays(3), "12345");
    var farskapserklaering1 =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            barn1,
            LocalDateTime.now().minusDays(14));

    var mor2Fnr =
        LocalDate.now().minusYears(23).format(DateTimeFormatter.ofPattern("ddMMyy")) + "60850";
    var mor2sAktoerid = "99996070809015";
    var barn2 = henteBarnUtenFnr(-3);
    var farskapserklaering2 =
        henteFarskapserklaering(
            Forelder.builder().foedselsnummer(mor2Fnr).build(),
            henteForelder(Forelderrolle.FAR),
            barn2,
            LocalDateTime.now().minusDays(3));

    var mor3Fnr =
        LocalDate.now().minusYears(45).format(DateTimeFormatter.ofPattern("ddMMyy")) + "73102";
    var mor3sAktoerid = "6481544698715";
    var barn3 = henteBarnUtenFnr(-3);
    var farskapserklaering3 =
        henteFarskapserklaering(
            Forelder.builder().foedselsnummer(mor3Fnr).build(),
            henteForelder(Forelderrolle.FAR),
            barn3,
            LocalDateTime.now().minusDays(8));

    farskapserklaering1.setFarBorSammenMedMor(false);
    farskapserklaering2.setFarBorSammenMedMor(false);
    farskapserklaering3.setFarBorSammenMedMor(false);

    persistenceService.lagreNyFarskapserklaering(farskapserklaering1);
    persistenceService.lagreNyFarskapserklaering(farskapserklaering2);
    persistenceService.lagreNyFarskapserklaering(farskapserklaering3);

    when(personopplysningService.henteAktoerid(farskapserklaering1.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(mor1sAktoerid));
    when(personopplysningService.henteAktoerid(farskapserklaering2.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(mor2sAktoerid));
    when(personopplysningService.henteAktoerid(farskapserklaering3.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(mor3sAktoerid));

    when(egenskaperOppgave.getMaksAntallOppgaverPerDag()).thenReturn(maksAntallErklaeringer);

    var idTilFarskapserklaeringerDetSkalOpprettesOppgaverFor =
        farskapserklaeringDao.henteIdTilFarskapserklaeringerDetSkalOpprettesOppgaverFor(
            LocalDate.now().minusWeeks(2), LocalDateTime.now().with(LocalTime.MIDNIGHT));

    // when
    oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var idTilFarskapserklaeringerSomGjenstaar =
        farskapserklaeringDao.henteIdTilFarskapserklaeringerDetSkalOpprettesOppgaverFor(
            LocalDate.now().minusWeeks(2), LocalDateTime.now().with(LocalTime.MIDNIGHT));

    assertAll(
        () -> assertThat(idTilFarskapserklaeringerDetSkalOpprettesOppgaverFor.size()).isEqualTo(3),
        () -> verify(oppgaveApiConsumer, times(2)).oppretteOppgave(any()),
        () -> assertThat(idTilFarskapserklaeringerSomGjenstaar.size()).isEqualTo(1));
  }

  @Test
  @DisplayName("Skal opprette - Nyfødt barn til foreldre som ikke bor sammen ved fødsel")
  void skalOppretteOppgaveForFarskapserklaeringSomGjelderNyfoedtBarnTilForeldreSomIkkeBorSammen() {

    // given
    var morsAktoerid = "40506070809010";
    var nyfoedtBarn = henteBarnMedFnr(LocalDate.now().minusDays(3), "12345");
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            nyfoedtBarn,
            LocalDateTime.now().minusDays(1));

    farskapserklaering.setFarBorSammenMedMor(false);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(personopplysningService.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    var oppgaveforespoerselfanger = ArgumentCaptor.forClass(Oppgaveforespoersel.class);
    when(egenskaperOppgave.getMaksAntallOppgaverPerDag()).thenReturn(10);
    when(oppgaveApiConsumer.oppretteOppgave(oppgaveforespoerselfanger.capture())).thenReturn(1234l);

    // when
    oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering =
        farskapserklaeringDao.findById(lagretFarskapserklaering.getId());
    var oppgaveforespoersel = oppgaveforespoerselfanger.getAllValues();

    var barn = farskapserklaering.getBarn();
    var far = farskapserklaering.getFar();
    var mor = farskapserklaering.getMor();

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> verify(oppgaveApiConsumer, times(1)).oppretteOppgave(any()),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNotNull(),
        () ->
            assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt())
                .isBefore(LocalDateTime.now().plusMinutes(10)),
        () ->
            assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt())
                .isAfter(LocalDateTime.now().minusMinutes(10)),
        () -> assertThat(oppgaveforespoersel.size()).isEqualTo(1),
        () ->
            assertThat(oppgaveforespoersel.get(0).getBeskrivelse())
                .isEqualTo(
                    String.format(
                        OPPGAVEBESKRIVELSE_GENERELL,
                        "barn med fødselsnummer " + barn.getFoedselsnummer(),
                        far.getFoedselsnummer(),
                        mor.getFoedselsnummer())),
        () -> assertThat(oppgaveforespoersel.get(0).getAktoerId()).isEqualTo(morsAktoerid),
        () -> assertThat(oppgaveforespoersel.get(0).getOppgavetype()).isEqualTo("GEN"),
        () -> assertThat(oppgaveforespoersel.get(0).getBehandlingstype()).isEqualTo("ae0118"),
        () -> assertThat(oppgaveforespoersel.get(0).getTildeltEnhetsnr()).isEqualTo("4860"),
        () -> assertThat(oppgaveforespoersel.get(0).getOpprettetAvEnhetsnr()).isEqualTo("9999"),
        () -> assertThat(oppgaveforespoersel.get(0).getPrioritet()).isEqualTo("NORM"),
        () -> assertThat(oppgaveforespoersel.get(0).getTema()).isEqualTo("BID"));
  }

  @Test
  @DisplayName("Skal opprette - Barn med termindato mer enn to uker tilbake i tid")
  void skalOppretteOppgaveForFarskapserklaeringSomGjelderBarnMedTermindatoMerEnnToUkerbakITid() {

    // given
    var morsAktoerid = "40506070809010";
    var ufoedtBarn = henteBarnUtenFnr(-3);
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            ufoedtBarn,
            LocalDateTime.now().minusDays(1));

    farskapserklaering.setFarBorSammenMedMor(false);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(personopplysningService.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    var oppgaveforespoerselfanger = ArgumentCaptor.forClass(Oppgaveforespoersel.class);

    when(egenskaperOppgave.getMaksAntallOppgaverPerDag()).thenReturn(10);
    when(oppgaveApiConsumer.oppretteOppgave(oppgaveforespoerselfanger.capture())).thenReturn(1234l);

    // when
    oppgavestyring.vurdereOpprettelseAvOppgave();

    when(egenskaperOppgave.getMaksAntallOppgaverPerDag()).thenReturn(10);

    // then
    var oppdatertFarskapserklaering =
        farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    var oppgaveforespoersel = oppgaveforespoerselfanger.getAllValues();
    var barn = farskapserklaering.getBarn();
    var far = farskapserklaering.getFar();
    var mor = farskapserklaering.getMor();

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> verify(oppgaveApiConsumer, times(1)).oppretteOppgave(any()),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNotNull(),
        () ->
            assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt())
                .isBefore(LocalDateTime.now().plusMinutes(10)),
        () ->
            assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt())
                .isAfter(LocalDateTime.now().minusMinutes(10)),
        () -> assertThat(oppgaveforespoersel.size()).isEqualTo(1),
        () ->
            assertThat(oppgaveforespoersel.get(0).getBeskrivelse())
                .isEqualTo(
                    String.format(
                        OPPGAVEBESKRIVELSE_GENERELL,
                        "barn oppgitt med termin "
                            + barn.getTermindato()
                                .format(
                                    DateTimeFormatter.ofPattern(OPPGAVE_DATOFORMAT_I_BESKRIVELSE)),
                        far.getFoedselsnummer(),
                        mor.getFoedselsnummer())),
        () -> assertThat(oppgaveforespoersel.get(0).getAktoerId()).isEqualTo(morsAktoerid),
        () -> assertThat(oppgaveforespoersel.get(0).getOppgavetype()).isEqualTo("GEN"),
        () -> assertThat(oppgaveforespoersel.get(0).getBehandlingstype()).isEqualTo("ae0118"),
        () -> assertThat(oppgaveforespoersel.get(0).getTildeltEnhetsnr()).isEqualTo("4860"),
        () -> assertThat(oppgaveforespoersel.get(0).getOpprettetAvEnhetsnr()).isEqualTo("9999"),
        () -> assertThat(oppgaveforespoersel.get(0).getPrioritet()).isEqualTo("NORM"),
        () -> assertThat(oppgaveforespoersel.get(0).getTema()).isEqualTo("BID"));
  }

  @Test
  @DisplayName("Skal ikke opprette - Barn med termindato mindre enn to uker tilbake i tid")
  void
      skalIkkeOppretteOppgaveForFarskapserklaeringSomGjelderBarnMedTermindatoMindreEnnToUkerbakITid() {

    // given
    var morsAktoerid = "40506070809010";
    var ufoedtBarn = henteBarnUtenFnr(-1);
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            ufoedtBarn,
            LocalDateTime.now().minusDays(1));

    farskapserklaering.setFarBorSammenMedMor(true);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(personopplysningService.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    // when
    oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering =
        farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> verify(oppgaveApiConsumer, times(0)).oppretteOppgave(any()),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNull());
  }

  @Test
  @DisplayName("Skal ikke opprette - Nyfødt barn til foreldre som bor sammen ved fødsel")
  void skalIkkeOppretteOppgaveForFarskapserklaeringSomGjelderNyfoedtBarnTilForeldreSomBorSammen() {

    // given
    var morsAktoerid = "40506070809010";
    var nyfoedtBarn = henteBarnMedFnr(LocalDate.now().minusDays(3), "12345");
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            nyfoedtBarn,
            LocalDateTime.now().minusDays(1));

    farskapserklaering.setFarBorSammenMedMor(true);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(personopplysningService.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    // when
    oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering =
        farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> verify(oppgaveApiConsumer, times(0)).oppretteOppgave(any()),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNull());
  }

  @Test
  @DisplayName("Skal ikke opprette - Farskapserklæring det allerede er opprettet oppgave for")
  void skalIkkeOppretteOppgaveForFarskapserklaeringSomDetAlleredeErOpprettetOppgaveFor() {

    // given
    var morsAktoerid = "40506070809010";
    var nyfoedtBarn = henteBarnMedFnr(LocalDate.now().minusDays(3), "12345");
    var farskapserklaering =
        henteFarskapserklaering(
            henteForelder(Forelderrolle.MOR),
            henteForelder(Forelderrolle.FAR),
            nyfoedtBarn,
            LocalDateTime.now().minusDays(1));
    var tidspunktOppgaveSendt = LocalDateTime.now().minusDays(2);

    farskapserklaering.setFarBorSammenMedMor(false);
    farskapserklaering.setOppgaveSendt(tidspunktOppgaveSendt);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(personopplysningService.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    // when
    oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering =
        farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNotNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> verify(oppgaveApiConsumer, times(0)).oppretteOppgave(any()),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNotNull(),
        () ->
            assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt())
                .isBetween(
                    tidspunktOppgaveSendt.minusSeconds(10), tidspunktOppgaveSendt.plusSeconds(10)));
  }

  private Farskapserklaering henteFarskapserklaering(
      Forelder mor, Forelder far, Barn barn, LocalDateTime signeringstidspunktFar) {

    var dokument =
        Dokument.builder()
            .navn("farskapserklaering.pdf")
            .signeringsinformasjonMor(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl("8080", "redirect-mor"))
                    .signeringstidspunkt(signeringstidspunktFar.minusMinutes(10))
                    .build())
            .signeringsinformasjonFar(
                Signeringsinformasjon.builder()
                    .redirectUrl(lageUrl("8080", "/redirect-far"))
                    .signeringstidspunkt(signeringstidspunktFar)
                    .build())
            .build();

    var farskapserklaering =
        Farskapserklaering.builder().barn(barn).mor(mor).far(far).dokument(dokument).build();
    farskapserklaering.setFarBorSammenMedMor(false);
    farskapserklaering.setMeldingsidSkatt(LocalDateTime.now().toString());
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    return farskapserklaering;
  }
}
