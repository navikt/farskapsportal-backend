package no.nav.farskapsportal.persistence.entity;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Barn")
@SpringBootTest(classes = Barn.class)
@ActiveProfiles(PROFILE_TEST)
public class BarnTest {

  @Test
  @DisplayName(
      "Likhetssjekk skal feile for barn uten fødselsnummer dersom relatert farskapserklæring gjelder andre foreldre")
  void
      likhetssjekkSkalFeileForBarnUtenFoedselsnummerDersomRelatertFarskapserklaeringGjelderAndreForeldre() {

    var fnrMor = "01018912345";
    var fnrFar = "01018532415";
    var fnrAnnenMor = "01019112345";
    var fnrAnnenFar = "01018765432";

    // given
    var mor =
        Forelder.builder().fornavn("Petra").etternavn("Buskerud").foedselsnummer(fnrMor).build();

    var far =
        Forelder.builder().fornavn("Smørjan").etternavn("Telemark").foedselsnummer(fnrFar).build();

    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();

    var signertErklaeringMor =
        SignertDokument.builder()
            .dokumentnavn("signertErklaeringMor.pdf")
            .signertDokument(
                String.format(
                    "Mor signerer farskapserklæring for barn med termindato %s",
                    barn.getTermindato())
                    .getBytes())
            .build();

    var farskapserklaering =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(mor)
            .far(far)
            .signertErklaeringMor(signertErklaeringMor)
            .build();

    barn.setFarskapserklaering(farskapserklaering);

    var annenFar =
        Forelder.builder()
            .foedselsnummer(fnrAnnenFar)
            .fornavn(far.getFornavn())
            .etternavn(far.getEtternavn())
            .build();
    var annenMor =
        Forelder.builder()
            .foedselsnummer(fnrAnnenMor)
            .fornavn(mor.getFornavn())
            .etternavn(mor.getEtternavn())
            .build();
    var annenFarskapserklaering =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(mor)
            .far(annenFar)
            .signertErklaeringMor(signertErklaeringMor)
            .build();

    var annetBarnMedSammeTermindato =
        Barn.builder()
            .termindato(barn.getTermindato())
            .farskapserklaering(annenFarskapserklaering)
            .build();

    // when, then
    assertAll(
        () ->
            assertFalse(
                barn.getFarskapserklaering().equals(annenFarskapserklaering),
                "Det ene barnets farskapserklæring skal ikke være lik det andre barnets farskapserklæring"),
        () ->
            assertFalse(
                barn.equals(annetBarnMedSammeTermindato),
                "To barn med samme termindato men forskjellige foreldre skal ikke kategoriseres som det samme barnet"));
  }

  @Test
  @DisplayName(
      "LikhetssjekkOkDersomBarnUtenFødselsnummerHarSammeTermindatoOgErRelatertTilSammeFarskapserklæring")
  void
      likhetssjekkOkDersomBarnUtenFoedselsnummerHarSammeTermindatoOgErRelatertTilSammeFarskapserklaering() {

    // given
    var mor =
        Forelder.builder()
            .fornavn("Petra")
            .etternavn("Buskerud")
            .foedselsnummer("01018912345")
            .build();

    var far =
        Forelder.builder()
            .fornavn("Smørjan")
            .etternavn("Telemark")
            .foedselsnummer("01018532415")
            .build();

    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();

    var signertErklaeringMor =
        SignertDokument.builder()
            .dokumentnavn("signertErklaeringMor.pdf")
            .signertDokument(
                String.format(
                    "Mor signerer farskapserklæring for barn med termindato %s",
                    barn.getTermindato())
                    .getBytes())
            .build();

    var barnFunksjonellKopi = Barn.builder().termindato(barn.getTermindato()).build();

    var farskapserklaering =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(mor)
            .far(far)
            .signertErklaeringMor(signertErklaeringMor)
            .build();

    var farskapserklaeringMedKopiAvBarn =
        Farskapserklaering.builder()
            .barn(barnFunksjonellKopi)
            .mor(mor)
            .far(far)
            .signertErklaeringMor(signertErklaeringMor)
            .build();

    barn.setFarskapserklaering(farskapserklaering);
    barnFunksjonellKopi.setFarskapserklaering(farskapserklaeringMedKopiAvBarn);

    // when, then
    assertAll(
        () ->
            assertTrue(
                barn.getFarskapserklaering().equals(barnFunksjonellKopi.getFarskapserklaering()), "Farskapserklæeringene skal være like"),
        () -> assertTrue(barn.equals(barnFunksjonellKopi), "Barna skal være like"));
  }

  @Test
  @DisplayName("Barn med forskjellige foreldre skal gi forskjellig hashkode")
  void barnMedForskjelligeForeldreSkalIkkeGiSammeHashkode() {

    // given
    /* en familie */
    var mor =
        Forelder.builder()
            .fornavn("Petra")
            .etternavn("Buskerud")
            .foedselsnummer("01018912345")
            .build();

    var far =
        Forelder.builder()
            .fornavn("Smørjan")
            .etternavn("Telemark")
            .foedselsnummer("01018532415")
            .build();

    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();

    var signertErklaeringMor =
        SignertDokument.builder()
            .dokumentnavn("signertErklaeringMor.pdf")
            .signertDokument(
                String.format(
                    "Mor signerer farskapserklæring for barn med termindato %s",
                    barn.getTermindato())
                    .getBytes())
            .build();

    var farskapserklaeringForEtBarn =
        Farskapserklaering.builder()
            .barn(barn)
            .mor(mor)
            .far(far)
            .signertErklaeringMor(signertErklaeringMor)
            .build();

    barn.setFarskapserklaering(farskapserklaeringForEtBarn);

    /* og en annen familie */
    var annenMor =
        Forelder.builder()
            .fornavn("Petra")
            .etternavn("Vesterålen")
            .foedselsnummer("01019512345")
            .build();

    var annenFar =
        Forelder.builder()
            .fornavn("Smørjan")
            .etternavn("Lofoten")
            .foedselsnummer("01018932415")
            .build();

    var annetBarnMedSammeTermindato = Barn.builder().termindato(barn.getTermindato()).build();

    var farskapserklaeringForEtAnnetBarn =
        Farskapserklaering.builder()
            .barn(annetBarnMedSammeTermindato)
            .mor(annenMor)
            .far(annenFar)
            .signertErklaeringMor(signertErklaeringMor)
            .build();

    barn.setFarskapserklaering(farskapserklaeringForEtAnnetBarn);

    // when, then
    assertTrue(
        barn.hashCode() != (annetBarnMedSammeTermindato.hashCode()),
        "Barn med samme termindato men forskjellige foreldre skal ikke ha samme hashkode");
  }

  @Test
  @DisplayName("Barn med samme termindato og samme foreldre skal ha samme hashkode")
  void barnMedSammeTermindatoOgSammeForeldreSkalHaSammeHashkode() {

    // given
    /* en familie */
    var mor =
        Forelder.builder()
            .fornavn("Petra")
            .etternavn("Buskerud")
            .foedselsnummer("01018912345")
            .build();

    var far =
        Forelder.builder()
            .fornavn("Smørjan")
            .etternavn("Telemark")
            .foedselsnummer("01018532415")
            .build();

    var detEneBarnet = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();

    var signertErklaeringMor =
        SignertDokument.builder()
            .dokumentnavn("signertErklaeringMor.pdf")
            .signertDokument(
                String.format(
                    "Mor signerer farskapserklæring for barn med termindato %s",
                    detEneBarnet.getTermindato())
                    .getBytes())
            .build();

    var detAndreBarnet = Barn.builder().termindato(detEneBarnet.getTermindato()).build();

    var farskapserklaeringForDetEneBarnet =
        Farskapserklaering.builder()
            .barn(detEneBarnet)
            .mor(mor)
            .far(far)
            .signertErklaeringMor(signertErklaeringMor)
            .build();

    detEneBarnet.setFarskapserklaering(farskapserklaeringForDetEneBarnet);

    var farskapserklaeringForDetAndreBarnet =
        Farskapserklaering.builder()
            .barn(detAndreBarnet)
            .mor(mor)
            .far(far)
            .signertErklaeringMor(signertErklaeringMor)
            .build();

    detAndreBarnet.setFarskapserklaering(farskapserklaeringForDetAndreBarnet);

    // when, then
    assertTrue(
        detEneBarnet.hashCode() == detAndreBarnet.hashCode(),
        "Barn med samme termindato og samme foreldre skal ha samme hashkode");
  }

  @Test
  @DisplayName("Barnets termindato skal være representert i streng-versjonen av en barn-instans")
  void barnetsTermindatoSkalVaereRepresentertIStrengversjonenAvEnBarninstans() {

  // given
  var mor =
      Forelder.builder()
          .fornavn("Petra")
          .etternavn("Buskerud")
          .foedselsnummer("01018912345")
          .build();

  var far =
      Forelder.builder()
          .fornavn("Smørjan")
          .etternavn("Telemark")
          .foedselsnummer("01018532415")
          .build();

  var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();

    var signertErklaeringMor =
        SignertDokument.builder()
            .dokumentnavn("signertErklaeringMor.pdf")
            .signertDokument(
                String.format(
                    "Mor signerer farskapserklæring for barn med termindato %s",
                    barn.getTermindato())
                    .getBytes())
            .build();

  var farskapserklaeringForDetEneBarnet =
      Farskapserklaering.builder()
          .barn(barn)
          .mor(mor)
          .far(far)
          .signertErklaeringMor(signertErklaeringMor)
          .build();

    barn.setFarskapserklaering(farskapserklaeringForDetEneBarnet);

    var barnToString = barn.toString();

    assertEquals(String.format("Barn knyttet til termindato: %s", barn.getTermindato().toString()),  barnToString);
 }
}
