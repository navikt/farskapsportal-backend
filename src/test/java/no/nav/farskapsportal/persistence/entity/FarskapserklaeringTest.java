package no.nav.farskapsportal.persistence.entity;

import static no.nav.farskapsportal.FarskapsportalApplicationLocal.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("Farskapserklaering")
@SpringBootTest(classes = Farskapserklaering.class)
@ActiveProfiles(PROFILE_TEST)
public class FarskapserklaeringTest {

  @Test
  @DisplayName("Skal gi ulike hashkoder dersom to farskapserklæringer ikke gjelder samme parter")
  void skalGiUlikeHashkoderDersomToFarskapserklaeringerIkkeGjelderSammeParter() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor =
        Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far =
        Forelder.builder().foedselsnummer("01018832145").fornavn("Roger").etternavn("Mer").build();

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
            .signertErklaering(signertErklaeringMor)
            .build();

    var etAnnetBarn = Barn.builder().termindato(barn.getTermindato()).build();
    var enAnnenMor =
        Forelder.builder().foedselsnummer("31019123450").fornavn("Greta").etternavn("Xyz").build();

    var enAnnenFarskapserklaering =
        Farskapserklaering.builder()
            .barn(etAnnetBarn)
            .mor(enAnnenMor)
            .far(far)
            .signertErklaering(signertErklaeringMor)
            .build();

    // when, then
    assertNotEquals(farskapserklaering.hashCode(), enAnnenFarskapserklaering.hashCode());
  }

  @Test
  @DisplayName("Skal gi like hashkoder dersom to farskapserklæringer gjelder samme parter")
  void skalGiLikeHashkoderDersomToFarskapserklaeringerGjelderSammeParter() {
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor =
        Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far =
        Forelder.builder().foedselsnummer("01038832140").fornavn("Roger").etternavn("Mer").build();
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
            .signertErklaering(signertErklaeringMor)
            .build();

    var barnMedSammeTermindato = Barn.builder().termindato(barn.getTermindato()).build();
    var sammeMor =
        Forelder.builder()
            .foedselsnummer(mor.getFoedselsnummer())
            .fornavn(mor.getFornavn())
            .etternavn(mor.getEtternavn())
            .build();
    var sammeFar =
        Forelder.builder()
            .foedselsnummer(far.getFoedselsnummer())
            .fornavn(far.getFornavn())
            .etternavn(far.getEtternavn())
            .build();

    var enAnnenFarskapserklaeringMedSammeParter =
        Farskapserklaering.builder()
            .barn(barnMedSammeTermindato)
            .mor(sammeMor)
            .far(sammeFar)
            .signertErklaering(signertErklaeringMor)
            .build();

    // when, then
    assertEquals(farskapserklaering.hashCode(), enAnnenFarskapserklaeringMedSammeParter.hashCode());
  }

  @Test
  @DisplayName(
      "To farskapserklæringer skal ikke kategoriseres som like dersom partene ikke er de samme")
  void farskapserklaeringerMedUlikeParterSkalKategoriseresSomUlike() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor =
        Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far =
        Forelder.builder().foedselsnummer("01018832145").fornavn("Roger").etternavn("Mer").build();
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
            .signertErklaering(signertErklaeringMor)
            .build();

    var etAnnetBarn = Barn.builder().termindato(barn.getTermindato()).build();
    var enAnnenMor =
        Forelder.builder().foedselsnummer("31019123450").fornavn("Greta").etternavn("Xyz").build();

    var enAnnenFarskapserklaering =
        Farskapserklaering.builder()
            .barn(etAnnetBarn)
            .mor(enAnnenMor)
            .far(far)
            .signertErklaering(signertErklaeringMor)
            .build();

    // when, then
    assertNotEquals(farskapserklaering, enAnnenFarskapserklaering);
  }

  @Test
  @DisplayName("To farskapserklæringer skal kategoriseres som like dersom alle parter er like")
  void farskapserklaeringerMedLikeParterSkalKategoriseresSomLike() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor =
        Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far =
        Forelder.builder().foedselsnummer("01038832140").fornavn("Roger").etternavn("Mer").build();
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
            .signertErklaering(signertErklaeringMor)
            .build();

    barn.setFarskapserklaering(farskapserklaering);
    mor.setErklaeringerMor(Set.of(farskapserklaering));
    far.setErklaeringerFar(Set.of(farskapserklaering));

    var barnMedSammeTermindato = Barn.builder().termindato(barn.getTermindato()).build();
    var sammeMor =
        Forelder.builder()
            .foedselsnummer(mor.getFoedselsnummer())
            .fornavn(mor.getFornavn())
            .etternavn(mor.getEtternavn())
            .build();
    var sammeFar =
        Forelder.builder()
            .foedselsnummer(far.getFoedselsnummer())
            .fornavn(far.getFornavn())
            .etternavn(far.getEtternavn())
            .build();

    var enAnnenFarskapserklaeringMedSammeParter =
        Farskapserklaering.builder()
            .barn(barnMedSammeTermindato)
            .mor(sammeMor)
            .far(sammeFar)
            .signertErklaering(signertErklaeringMor)
            .build();

    barnMedSammeTermindato.setFarskapserklaering(enAnnenFarskapserklaeringMedSammeParter);
    sammeMor.setErklaeringerMor(Set.of(enAnnenFarskapserklaeringMedSammeParter));
    sammeFar.setErklaeringerFar(Set.of(enAnnenFarskapserklaeringMedSammeParter));

    // when, then
    assertEquals(farskapserklaering, enAnnenFarskapserklaeringMedSammeParter);
  }

  @Test
  @DisplayName("Teste toString")
  void testeToString() {

    // given
    var barn = Barn.builder().termindato(LocalDate.now().plusMonths(6)).build();
    var mor =
        Forelder.builder().foedselsnummer("01019012345").fornavn("Petra").etternavn("Busk").build();
    var far =
        Forelder.builder().foedselsnummer("01038832140").fornavn("Roger").etternavn("Mer").build();
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
            .signertErklaering(signertErklaeringMor)
            .build();

    barn.setFarskapserklaering(farskapserklaering);
    mor.setErklaeringerMor(Set.of(farskapserklaering));
    far.setErklaeringerFar(Set.of(farskapserklaering));

    // when
    var toString = farskapserklaering.toString();

    // then
    assertEquals(
        "Farskapserklaering gjelder barn med termindato "
            + barn.getTermindato()
            + "\nMor: "
            + mor.getFornavn()
            + " "
            + mor.getEtternavn()
            + "\nFar: "
            + far.getFornavn()
            + " "
            + far.getEtternavn(),
        toString);
  }
}
