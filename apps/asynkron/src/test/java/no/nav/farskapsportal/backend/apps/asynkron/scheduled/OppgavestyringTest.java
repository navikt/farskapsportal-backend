package no.nav.farskapsportal.backend.apps.asynkron.scheduled;

import static no.nav.farskapsportal.backend.libs.felles.config.FarskapsportalFellesConfig.PROFILE_TEST;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnMedFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteBarnUtenFnr;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.henteForelder;
import static no.nav.farskapsportal.backend.libs.felles.test.utils.TestUtils.lageUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import no.nav.farskapsportal.backend.apps.asynkron.FarskapsportalAsynkronTestApplication;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.api.FarskapsportalApiConsumer;
import no.nav.farskapsportal.backend.apps.asynkron.consumer.oppgave.OppgaveApiConsumer;
import no.nav.farskapsportal.backend.libs.dto.Forelderrolle;
import no.nav.farskapsportal.backend.libs.dto.oppgave.Oppgaveforespoersel;
import no.nav.farskapsportal.backend.libs.entity.Barn;
import no.nav.farskapsportal.backend.libs.entity.Dokument;
import no.nav.farskapsportal.backend.libs.entity.Dokumentinnhold;
import no.nav.farskapsportal.backend.libs.entity.Farskapserklaering;
import no.nav.farskapsportal.backend.libs.entity.Forelder;
import no.nav.farskapsportal.backend.libs.entity.Signeringsinformasjon;
import no.nav.farskapsportal.backend.libs.felles.persistence.dao.FarskapserklaeringDao;
import no.nav.farskapsportal.backend.libs.felles.service.PersistenceService;
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
@ActiveProfiles(PROFILE_TEST)
@SpringBootTest(classes = FarskapsportalAsynkronTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class OppgavestyringTest {

  private static final String OPPGAVE_DATOFORMAT_I_BESKRIVELSE = "dd.MM.YYYY";

  private static final String OPPGAVEBESKRIVELSE_GENERELL = "ELEKTRONISK ERKLÆRING -"
      + " Farskap for %s er erklært elektronisk. Far har oppgitt at han ikke bor sammen med mor. Vurder om det skal tas opp bidragssak.";

  private @Autowired PersistenceService persistenceService;
  private @Autowired FarskapserklaeringDao farskapserklaeringDao;
  private @MockBean FarskapsportalApiConsumer farskapsportalApiConsumer;
  private @MockBean OppgaveApiConsumer oppgaveApiConsumer;
  private Oppgavestyring oppgavestyring;

  @BeforeEach
  void setup() {
    farskapserklaeringDao.deleteAll();

    oppgavestyring = Oppgavestyring.builder()
        .farskapsportalApiConsumer(farskapsportalApiConsumer)
        .oppgaveApiConsumer(oppgaveApiConsumer)
        .farskapserklaeringDao(farskapserklaeringDao).build();
  }

  @Test
  @DisplayName("Skal opprette - Nyfødt barn til foreldre som ikke bor sammen ved fødsel")
  void skalOppretteOppgaveForFarskapserklaeringSomGjelderNyfoedtBarnTilForeldreSomIkkeBorSammen() {

    // given
    var morsAktoerid = "40506070809010";
    var nyfoedtBarn = henteBarnMedFnr(LocalDate.now().minusDays(3), "12345");
    var farskapserklaering = henteFarskapserklaering(
        henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), nyfoedtBarn, LocalDateTime.now().minusDays(1));

    farskapserklaering.setFarBorSammenMedMor(false);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(farskapsportalApiConsumer.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    var oppgaveforespoerselfanger = ArgumentCaptor.forClass(Oppgaveforespoersel.class);

    // when
    var oppgaveOpprettetForAntallFarskapserklaeringer = oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());
    var oppgaveforespoersel = oppgaveforespoerselfanger.getAllValues();

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> assertThat(oppgaveOpprettetForAntallFarskapserklaeringer).isEqualTo(1),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNotNull(),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isBefore(LocalDateTime.now().plusMinutes(10)),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isAfter(LocalDateTime.now().minusMinutes(10)),
        () -> assertThat(oppgaveforespoersel.size()).isEqualTo(1),
        () -> assertThat(oppgaveforespoersel.get(0).getBeskrivelse()).isEqualTo(String.format(OPPGAVEBESKRIVELSE_GENERELL,
            "barn oppgitt med fødselsnummer " + farskapserklaering.getBarn().getFoedselsnummer())),
        () -> assertThat(oppgaveforespoersel.get(0).getAktoerId()).isEqualTo(morsAktoerid),
        () -> assertThat(oppgaveforespoersel.get(0).getOppgavetype()).isEqualTo("GEN"),
        () -> assertThat(oppgaveforespoersel.get(0).getBehandlingstype()).isEqualTo("ae0118"),
        () -> assertThat(oppgaveforespoersel.get(0).getTildeltEnhetsnr()).isEqualTo("4860"),
        () -> assertThat(oppgaveforespoersel.get(0).getOpprettetAvEnhetsnr()).isEqualTo("9999"),
        () -> assertThat(oppgaveforespoersel.get(0).getPrioritet()).isEqualTo("NORM"),
        () -> assertThat(oppgaveforespoersel.get(0).getTema()).isEqualTo("BID")
    );
  }

  @Test
  @DisplayName("Skal opprette - Barn med termindato mer enn to uker tilbake i tid")
  void skalOppretteOppgaveForFarskapserklaeringSomGjelderBarnMedTermindatoMerEnnToUkerbakITid() {

    // given
    var morsAktoerid = "40506070809010";
    var ufoedtBarn = henteBarnUtenFnr(-3);
    var farskapserklaering = henteFarskapserklaering(
        henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), ufoedtBarn, LocalDateTime.now().minusDays(1));

    farskapserklaering.setFarBorSammenMedMor(false);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(farskapsportalApiConsumer.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    var oppgaveforespoerselfanger = ArgumentCaptor.forClass(Oppgaveforespoersel.class);

    // when
    when(oppgaveApiConsumer.oppretteOppgave(oppgaveforespoerselfanger.capture())).thenReturn(1234l);
    var oppgaveOpprettetForAntallFarskapserklaeringer = oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    var oppgaveforespoersel = oppgaveforespoerselfanger.getAllValues();

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> assertThat(oppgaveOpprettetForAntallFarskapserklaeringer).isEqualTo(1),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNotNull(),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isBefore(LocalDateTime.now().plusMinutes(10)),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isAfter(LocalDateTime.now().minusMinutes(10)),
        () -> assertThat(oppgaveforespoersel.size()).isEqualTo(1),
        () -> assertThat(oppgaveforespoersel.get(0).getBeskrivelse()).isEqualTo(String.format(OPPGAVEBESKRIVELSE_GENERELL,
                "barn oppgitt med termin " + farskapserklaering.getBarn().getTermindato().format(DateTimeFormatter.ofPattern( OPPGAVE_DATOFORMAT_I_BESKRIVELSE)))),
        () -> assertThat(oppgaveforespoersel.get(0).getAktoerId()).isEqualTo(morsAktoerid),
        () -> assertThat(oppgaveforespoersel.get(0).getOppgavetype()).isEqualTo("GEN"),
        () -> assertThat(oppgaveforespoersel.get(0).getBehandlingstype()).isEqualTo("ae0118"),
        () -> assertThat(oppgaveforespoersel.get(0).getTildeltEnhetsnr()).isEqualTo("4860"),
        () -> assertThat(oppgaveforespoersel.get(0).getOpprettetAvEnhetsnr()).isEqualTo("9999"),
        () -> assertThat(oppgaveforespoersel.get(0).getPrioritet()).isEqualTo("NORM"),
        () -> assertThat(oppgaveforespoersel.get(0).getTema()).isEqualTo("BID")
    );
  }

  @Test
  @DisplayName("Skal ikke opprette - Barn med termindato mindre enn to uker tilbake i tid")
  void skalIkkeOppretteOppgaveForFarskapserklaeringSomGjelderBarnMedTermindatoMindreEnnToUkerbakITid() {

    // given
    var morsAktoerid = "40506070809010";
    var ufoedtBarn = henteBarnUtenFnr(-1);
    var farskapserklaering = henteFarskapserklaering(
        henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), ufoedtBarn, LocalDateTime.now().minusDays(1));

    farskapserklaering.setFarBorSammenMedMor(true);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(farskapsportalApiConsumer.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    // when
    var oppgaveOpprettetForAntallFarskapserklaeringer = oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> assertThat(oppgaveOpprettetForAntallFarskapserklaeringer).isEqualTo(0),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNull()
    );
  }

  @Test
  @DisplayName("Skal ikke opprette - Nyfødt barn til foreldre som bor sammen ved fødsel")
  void skalIkkeOppretteOppgaveForFarskapserklaeringSomGjelderNyfoedtBarnTilForeldreSomBorSammen() {

    // given
    var morsAktoerid = "40506070809010";
    var nyfoedtBarn = henteBarnMedFnr(LocalDate.now().minusDays(3), "12345");
    var farskapserklaering = henteFarskapserklaering(
        henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), nyfoedtBarn, LocalDateTime.now().minusDays(1));

    farskapserklaering.setFarBorSammenMedMor(false);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(farskapsportalApiConsumer.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    // when
    var oppgaveOpprettetForAntallFarskapserklaeringer = oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> assertThat(oppgaveOpprettetForAntallFarskapserklaeringer).isEqualTo(1),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNotNull(),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isBefore(LocalDateTime.now().plusMinutes(10)),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isAfter(LocalDateTime.now().minusMinutes(10))
    );
  }

  @Test
  @DisplayName("Skal ikke opprette - Farskapserklæring det allerede er opprettet oppgave for")
  void skalIkkeOppretteOppgaveForFarskapserklaeringSomGDetAlleredeErOpprettetOppgaveFor() {

    // given
    var morsAktoerid = "40506070809010";
    var nyfoedtBarn = henteBarnMedFnr(LocalDate.now().minusDays(3), "12345");
    var farskapserklaering = henteFarskapserklaering(
        henteForelder(Forelderrolle.MOR), henteForelder(Forelderrolle.FAR), nyfoedtBarn, LocalDateTime.now().minusDays(1));
    var tidspunktOppgaveSendt = LocalDateTime.now().minusDays(2);

    farskapserklaering.setFarBorSammenMedMor(false);
    farskapserklaering.setOppgaveSendt(tidspunktOppgaveSendt);

    var lagretFarskapserklaering = persistenceService.lagreNyFarskapserklaering(farskapserklaering);
    when(farskapsportalApiConsumer.henteAktoerid(farskapserklaering.getMor().getFoedselsnummer()))
        .thenReturn(Optional.of(morsAktoerid));

    // when
    var oppgaveOpprettetForAntallFarskapserklaeringer = oppgavestyring.vurdereOpprettelseAvOppgave();

    // then
    var oppdatertFarskapserklaering = farskapserklaeringDao.findById(lagretFarskapserklaering.getId());

    assertAll(
        () -> assertThat(lagretFarskapserklaering.getOppgaveSendt()).isNotNull(),
        () -> assertThat(oppdatertFarskapserklaering.isPresent()).isTrue(),
        () -> assertThat(oppgaveOpprettetForAntallFarskapserklaeringer).isEqualTo(0),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isNotNull(),
        () -> assertThat(oppdatertFarskapserklaering.get().getOppgaveSendt()).isBetween(tidspunktOppgaveSendt.minusSeconds(10),
            tidspunktOppgaveSendt.plusSeconds(10))
    );
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
    farskapserklaering.setFarBorSammenMedMor(false);
    farskapserklaering.setMeldingsidSkatt(LocalDateTime.now().toString());
    farskapserklaering.setSendtTilSkatt(LocalDateTime.now());

    return farskapserklaering;
  }
}
